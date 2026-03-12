const { default: makeWASocket, DisconnectReason, useMultiFileAuthState } = require('@whiskeysockets/baileys')
const { Boom } = require('@hapi/boom')
const express = require('express')
const { WebSocketServer } = require('ws')
const http = require('http')
const qrcode = require('qrcode')

const app = express()
const server = http.createServer(app)
const wss = new WebSocketServer({ server })

let currentQR = null
const clients = new Set()

app.get('/health', (req, res) => res.json({ status: 'ok' }))

app.get('/qr', async (req, res) => {
  if (!currentQR) return res.json({ qr: null, status: 'waiting' })
  try {
    const qrDataUrl = await qrcode.toDataURL(currentQR)
    res.json({ qr: qrDataUrl, raw: currentQR, status: 'ready' })
  } catch (e) {
    res.status(500).json({ error: e.message })
  }
})

wss.on('connection', (ws) => {
  clients.add(ws)
  console.log('Watch connected, clients:', clients.size)
  if (currentQR) sendQR(ws, currentQR)
  ws.on('close', () => clients.delete(ws))
})

async function sendQR(ws, raw) {
  try {
    const qrDataUrl = await qrcode.toDataURL(raw, { width: 300 })
    if (ws.readyState === 1) ws.send(JSON.stringify({ type: 'qr', data: qrDataUrl }))
  } catch (e) {}
}

function broadcast(raw) {
  clients.forEach(ws => sendQR(ws, raw))
}

async function startWhatsApp() {
  const { state, saveCreds } = await useMultiFileAuthState('auth_info')

  const sock = makeWASocket({
    auth: state,
    printQRInTerminal: true,
    browser: ['WhatsApp Watch', 'Chrome', '1.0']
  })

  sock.ev.on('creds.update', saveCreds)

  sock.ev.on('connection.update', async ({ connection, lastDisconnect, qr }) => {
    if (qr) {
      console.log('New QR received, broadcasting to', clients.size, 'clients')
      currentQR = qr
      broadcast(qr)
    }

    if (connection === 'close') {
      currentQR = null
      const shouldReconnect = (lastDisconnect?.error)?.output?.statusCode !== DisconnectReason.loggedOut
      if (shouldReconnect) {
        console.log('Reconnecting...')
        setTimeout(startWhatsApp, 3000)
      }
    }

    if (connection === 'open') {
      currentQR = null
      clients.forEach(ws => ws.send(JSON.stringify({ type: 'connected' })))
      console.log('WhatsApp connected!')
    }
  })
}

const PORT = process.env.PORT || 3000
server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`)
  startWhatsApp()
})
