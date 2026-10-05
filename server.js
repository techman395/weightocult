const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const ICON_PATH = path.join(__dirname, 'app/src/main/res/drawable/app_icon.png');

function findApkFile() {
  const candidates = [
    path.join(__dirname, 'release/WeightOCult-v1.0.1.apk'),
    path.join(__dirname, 'release/WeightOCult-v1.0.0.apk'),
    path.join(__dirname, 'app/build/outputs/apk/debug/app-debug.apk'),
    path.join(__dirname, 'app/build/outputs/apk/release/app-release.apk')
  ];
  for (const c of candidates) {
    if (fs.existsSync(c)) return c;
  }
  return null;
}

const server = http.createServer((req, res) => {
  const url = req.url.split('?')[0];

  // APK download routes
  if (url === '/weightocult-debug.apk' || url === '/weightocult.apk' || url === '/app-debug.apk' || url === '/download' || url === '/occult.apk' || url === '/WeightOCult-v1.0.0.apk' || url === '/WeightOCult-v1.0.1.apk' || url === '/WeightOCult-latest.apk') {
    const apkFile = findApkFile();
    if (apkFile) {
      const stat = fs.statSync(apkFile);
      const filename = path.basename(apkFile);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': `attachment; filename="${filename}"`,
        'Content-Length': stat.size,
        'Cache-Control': 'no-cache'
      });
      return fs.createReadStream(apkFile).pipe(res);
    }
    // Fallback: direct redirect to live GitHub release asset
    res.writeHead(302, {
      Location: 'https://github.com/techman395/weightocult/releases/download/v1.0.1/WeightOCult-v1.0.1.apk'
    });
    return res.end();
  }

  // App icon route
  if (url === '/app_icon.png') {
    if (fs.existsSync(ICON_PATH)) {
      const stat = fs.statSync(ICON_PATH);
      res.writeHead(200, {
        'Content-Type': 'image/png',
        'Content-Length': stat.size,
        'Cache-Control': 'public, max-age=86400'
      });
      return fs.createReadStream(ICON_PATH).pipe(res);
    }
  }

  // Health check
  if (url === '/health') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    return res.end('OK');
  }

  // Landing page
  let apkSizeMb = '22.8';
  try {
    if (fs.existsSync(APK_PATH)) {
      const stat = fs.statSync(APK_PATH);
      apkSizeMb = (stat.size / (1024 * 1024)).toFixed(1);
    }
  } catch (e) {}

  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>WeightOCult — Privacy-First Cult Smart Scale App</title>
  <meta name="description" content="Open-source, 100% offline Android telemetry client for Cult smart scales. Peer-reviewed body composition calculations with zero cloud tracking."/>
  <link rel="icon" type="image/png" href="/app_icon.png" />
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #07070b;
      --bg-card: rgba(18, 18, 28, 0.7);
      --bg-card-hover: rgba(26, 26, 40, 0.85);
      --border: rgba(255, 255, 255, 0.08);
      --border-bright: rgba(0, 212, 255, 0.35);
      --cyan: #00d4ff;
      --cyan-glow: rgba(0, 212, 255, 0.35);
      --purple: #8b5cf6;
      --mint: #10b981;
      --amber: #f59e0b;
      --text: #f8fafc;
      --text-muted: #94a3b8;
      --text-subtle: #64748b;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    html { scroll-behavior: smooth; }
    body {
      background: var(--bg);
      color: var(--text);
      font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      min-height: 100vh;
      line-height: 1.5;
      overflow-x: hidden;
      background-image: 
        radial-gradient(ellipse 80% 50% at 50% -20%, rgba(0, 212, 255, 0.15), transparent),
        radial-gradient(ellipse 60% 40% at 85% 60%, rgba(139, 92, 246, 0.1), transparent);
    }

    /* Container */
    .wrapper {
      max-width: 1140px;
      margin: 0 auto;
      padding: 0 24px;
    }

    /* Navbar */
    nav {
      position: sticky;
      top: 0;
      z-index: 100;
      backdrop-filter: blur(16px);
      background: rgba(7, 7, 11, 0.8);
      border-bottom: 1px solid var(--border);
    }
    .nav-inner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 72px;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
      color: var(--text);
    }
    .brand-logo {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      box-shadow: 0 4px 12px rgba(0, 212, 255, 0.25);
    }
    .brand-title {
      font-size: 20px;
      font-weight: 800;
      letter-spacing: -0.5px;
      background: linear-gradient(135deg, #fff 40%, var(--cyan));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .nav-link {
      color: var(--text-muted);
      text-decoration: none;
      font-size: 14px;
      font-weight: 600;
      transition: color 0.2s;
    }
    .nav-link:hover { color: var(--text); }
    .btn-github-nav {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      border-radius: 9999px;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid var(--border);
      color: var(--text);
      font-size: 13px;
      font-weight: 600;
      text-decoration: none;
      transition: all 0.2s;
    }
    .btn-github-nav:hover {
      background: rgba(255, 255, 255, 0.12);
      border-color: rgba(255, 255, 255, 0.2);
      transform: translateY(-1px);
    }

    /* Hero Section */
    .hero {
      padding: 80px 0 60px;
      text-align: center;
      position: relative;
    }
    .pill-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 6px 16px;
      border-radius: 9999px;
      background: rgba(0, 212, 255, 0.1);
      border: 1px solid rgba(0, 212, 255, 0.25);
      color: var(--cyan);
      font-size: 13px;
      font-weight: 600;
      margin-bottom: 24px;
    }
    .pill-badge span.dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--cyan);
      box-shadow: 0 0 8px var(--cyan);
    }
    .hero-h1 {
      font-size: clamp(38px, 6vw, 64px);
      font-weight: 800;
      line-height: 1.1;
      letter-spacing: -1.5px;
      margin-bottom: 20px;
      color: #ffffff;
    }
    .hero-h1 span.gradient {
      background: linear-gradient(135deg, #00d4ff 10%, #a855f7 90%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .hero-desc {
      font-size: clamp(16px, 2vw, 19px);
      color: var(--text-muted);
      max-width: 680px;
      margin: 0 auto 36px;
      line-height: 1.6;
    }
    .hero-cta {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: center;
      gap: 16px;
      margin-bottom: 28px;
    }
    .btn-primary {
      display: inline-flex;
      align-items: center;
      gap: 12px;
      padding: 16px 32px;
      border-radius: 14px;
      font-size: 16px;
      font-weight: 700;
      background: linear-gradient(135deg, #00d4ff 0%, #2563eb 100%);
      color: #ffffff;
      text-decoration: none;
      box-shadow: 0 10px 30px rgba(0, 212, 255, 0.4);
      transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 14px 40px rgba(0, 212, 255, 0.6);
    }
    .btn-secondary {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      padding: 16px 28px;
      border-radius: 14px;
      font-size: 16px;
      font-weight: 600;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--border);
      color: var(--text);
      text-decoration: none;
      transition: all 0.2s;
    }
    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.25);
      transform: translateY(-2px);
    }
    .hero-meta {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 24px;
      font-size: 13px;
      color: var(--text-subtle);
    }
    .hero-meta span {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    /* Live Interactive Telemetry Preview */
    .preview-card {
      margin: 40px auto 80px;
      max-width: 820px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: 24px;
      padding: 32px;
      box-shadow: 0 25px 60px -15px rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(12px);
    }
    .preview-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid var(--border);
      padding-bottom: 20px;
      margin-bottom: 24px;
    }
    .preview-scale-status {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 14px;
      font-weight: 600;
    }
    .status-indicator {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: var(--mint);
      box-shadow: 0 0 10px var(--mint);
      animation: pulse 2s infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.5; transform: scale(0.9); }
    }
    .scale-display {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      margin-bottom: 24px;
    }
    @media (max-width: 640px) {
      .scale-display { grid-template-columns: 1fr; }
    }
    .dial-box {
      background: rgba(0, 0, 0, 0.4);
      border: 1px solid var(--border);
      border-radius: 18px;
      padding: 28px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
    }
    .dial-weight {
      font-size: 56px;
      font-weight: 800;
      letter-spacing: -2px;
      color: #fff;
      font-family: 'JetBrains Mono', monospace;
    }
    .dial-unit {
      font-size: 18px;
      color: var(--cyan);
      font-weight: 700;
      margin-left: 4px;
    }
    .dial-label {
      font-size: 13px;
      color: var(--text-muted);
      margin-top: 4px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }
    .telemetry-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }
    .telemetry-tile {
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid var(--border);
      border-radius: 14px;
      padding: 14px 16px;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }
    .telemetry-tile .lbl {
      font-size: 11px;
      font-weight: 600;
      color: var(--text-subtle);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .telemetry-tile .val {
      font-size: 20px;
      font-weight: 700;
      color: var(--text);
      margin-top: 4px;
    }
    .telemetry-tile .sub {
      font-size: 11px;
      color: var(--cyan);
      margin-top: 2px;
    }

    /* Bento Features */
    .section-head {
      text-align: center;
      margin-bottom: 48px;
    }
    .section-head h2 {
      font-size: 36px;
      font-weight: 800;
      letter-spacing: -1px;
      margin-bottom: 12px;
      color: #fff;
    }
    .section-head p {
      font-size: 16px;
      color: var(--text-muted);
      max-width: 580px;
      margin: 0 auto;
    }
    .bento-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 24px;
      margin-bottom: 80px;
    }
    @media (max-width: 900px) {
      .bento-grid { grid-template-columns: repeat(2, 1fr); }
    }
    @media (max-width: 600px) {
      .bento-grid { grid-template-columns: 1fr; }
    }
    .bento-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: 20px;
      padding: 28px;
      transition: all 0.25s ease;
      display: flex;
      flex-direction: column;
    }
    .bento-card:hover {
      background: var(--bg-card-hover);
      border-color: var(--border-bright);
      transform: translateY(-3px);
    }
    .bento-icon {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      background: rgba(0, 212, 255, 0.1);
      border: 1px solid rgba(0, 212, 255, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
      color: var(--cyan);
    }
    .bento-card h3 {
      font-size: 18px;
      font-weight: 700;
      margin-bottom: 8px;
      color: #fff;
    }
    .bento-card p {
      font-size: 14px;
      color: var(--text-muted);
      line-height: 1.6;
    }

    /* Models Table */
    .table-container {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: 20px;
      overflow: hidden;
      margin-bottom: 80px;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }
    th {
      background: rgba(255, 255, 255, 0.03);
      padding: 18px 24px;
      font-size: 13px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--text-muted);
      border-bottom: 1px solid var(--border);
    }
    td {
      padding: 18px 24px;
      font-size: 14px;
      border-bottom: 1px solid var(--border);
      color: var(--text);
    }
    tr:last-child td { border-bottom: none; }
    .badge-check {
      color: var(--mint);
      font-weight: 600;
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
    .badge-manual {
      color: var(--amber);
      font-weight: 600;
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }

    /* GitHub Section */
    .github-banner {
      background: linear-gradient(135deg, rgba(30, 27, 50, 0.9), rgba(12, 11, 24, 0.9));
      border: 1px solid rgba(139, 92, 246, 0.3);
      border-radius: 24px;
      padding: 48px;
      text-align: center;
      margin-bottom: 80px;
      position: relative;
      overflow: hidden;
    }
    .github-banner h2 {
      font-size: 32px;
      font-weight: 800;
      margin-bottom: 12px;
      color: #fff;
    }
    .github-banner p {
      color: var(--text-muted);
      font-size: 16px;
      max-width: 600px;
      margin: 0 auto 28px;
    }
    .github-cta-group {
      display: flex;
      justify-content: center;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
    }
    .btn-github-large {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      padding: 14px 28px;
      border-radius: 12px;
      background: #ffffff;
      color: #000000;
      font-weight: 700;
      font-size: 15px;
      text-decoration: none;
      transition: all 0.2s;
    }
    .btn-github-large:hover {
      background: #e2e8f0;
      transform: translateY(-2px);
      box-shadow: 0 10px 25px rgba(255, 255, 255, 0.2);
    }

    /* Footer */
    footer {
      border-top: 1px solid var(--border);
      padding: 40px 0;
      color: var(--text-subtle);
      font-size: 14px;
    }
    .footer-inner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 20px;
    }
    .footer-links {
      display: flex;
      gap: 24px;
    }
    .footer-links a {
      color: var(--text-muted);
      text-decoration: none;
      transition: color 0.2s;
    }
    .footer-links a:hover { color: var(--text); }
  </style>
</head>
<body>

  <!-- Navigation -->
  <nav>
    <div class="wrapper nav-inner">
      <a href="#" class="brand">
        <img src="/app_icon.png" alt="WeightOCult" class="brand-logo" />
        <span class="brand-title">WeightOCult</span>
      </a>
      <div class="nav-links">
        <a href="#features" class="nav-link">Features</a>
        <a href="#compatibility" class="nav-link">Compatibility</a>
        <a href="https://github.com/techman395/weightocult" target="_blank" class="btn-github-nav">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
          </svg>
          GitHub
        </a>
      </div>
    </div>
  </nav>

  <!-- Hero -->
  <section class="hero wrapper">
    <div class="pill-badge">
      <span class="dot"></span>
      <span>v1.0.1 Release Ready • Open Source</span>
    </div>

    <h1 class="hero-h1">
      Your Cult Smart Scale.<br/>
      <span class="gradient">Zero Cloud. Pure Privacy.</span>
    </h1>

    <p class="hero-desc">
      An independent, open-source Android client for Cult smart scales. Connects directly via Bluetooth Low Energy, decodes raw bioimpedance, and calculates honest peer-reviewed body composition completely on-device.
    </p>

    <div class="hero-cta">
      <a href="/WeightOCult-v1.0.1.apk" class="btn-primary">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="7 10 12 15 17 10"></polyline>
          <line x1="12" y1="15" x2="12" y2="3"></line>
        </svg>
        Download APK v1.0.1 (${apkSizeMb} MB)
      </a>

      <a href="https://github.com/techman395/weightocult" target="_blank" class="btn-secondary">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
        </svg>
        View on GitHub
      </a>
    </div>

    <div class="hero-meta">
      <span>✓ Android 8.0+</span>
      <span>✓ Zero Accounts or Logins</span>
      <span>✓ 100% Offline Storage</span>
    </div>

    <!-- Live Telemetry Simulator Card -->
    <div class="preview-card">
      <div class="preview-header">
        <div class="preview-scale-status">
          <span class="status-indicator"></span>
          <span>Live BLE Telemetry • Cult Smart Scale Connected</span>
        </div>
        <span style="font-size: 12px; color: var(--text-subtle); font-family: monospace;">UUID 0xFFF0 / 0xFFF4</span>
      </div>

      <div class="scale-display">
        <div class="dial-box">
          <div>
            <span class="dial-weight" id="liveWeight">72.4</span>
            <span class="dial-unit">kg</span>
          </div>
          <div class="dial-label">Stabilized Body Weight</div>
        </div>

        <div class="telemetry-grid">
          <div class="telemetry-tile">
            <span class="lbl">Lean Mass (FFM)</span>
            <span class="val" id="liveLean">58.8 kg</span>
            <span class="sub">Muscle + Water + Bone</span>
          </div>
          <div class="telemetry-tile">
            <span class="lbl">Body Fat</span>
            <span class="val" id="liveFat">18.8%</span>
            <span class="sub">Deurenberg (1991)</span>
          </div>
          <div class="telemetry-tile">
            <span class="lbl">Skeletal Muscle</span>
            <span class="val" id="liveMuscle">32.1 kg</span>
            <span class="sub">Janssen BIA Formula</span>
          </div>
          <div class="telemetry-tile">
            <span class="lbl">Heart Rate</span>
            <span class="val" id="liveHr">68 BPM</span>
            <span class="sub">Optical Pulse Sensor</span>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- Features Grid -->
  <section class="wrapper" id="features">
    <div class="section-head">
      <h2>Engineered for Privacy & Accuracy</h2>
      <p>Built for users who want clean health telemetry without corporate accounts, advertising, or locked-in cloud servers.</p>
    </div>

    <div class="bento-grid">
      <div class="bento-card">
        <div class="bento-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M6.5 6.5l11 11L12 23V1l5.5 5.5-11 11"></path>
          </svg>
        </div>
        <h3>Direct BLE Protocol</h3>
        <p>Connects directly to Cult smart scales using the native BLE GATT Service <code>0xFFF0</code>. No bridge app, zero registration, and zero proprietary pairing tokens needed.</p>
      </div>

      <div class="bento-card">
        <div class="bento-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
          </svg>
        </div>
        <h3>100% Offline & Private</h3>
        <p>Every weigh-in and bioimpedance packet stays securely inside your phone’s local database. No servers, no tracking analytics, and no mandatory cloud login.</p>
      </div>

      <div class="bento-card">
        <div class="bento-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
        </div>
        <h3>Peer-Reviewed Biometrics</h3>
        <p>Implements verified formulas: Janssen (2000) for Skeletal Muscle, Deurenberg (1991) for Body Fat %, Watson (1980) for Total Body Water, and Mifflin-St Jeor (1990) for BMR.</p>
      </div>

      <div class="bento-card">
        <div class="bento-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M12 16v-4"></path>
            <path d="M12 8h.01"></path>
          </svg>
        </div>
        <h3>Honest "Lean Mass"</h3>
        <p>Commercial scales label Fat-Free Mass as "muscle" to flatter users. WeightOCult clearly shows Lean Mass as the sum of muscle, water, and bone weight.</p>
      </div>

      <div class="bento-card">
        <div class="bento-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <h3>Multi-User Profiles</h3>
        <p>Create separate biometric profiles for family members. Customize target weights, age, height, gender, and personal color themes with one-tap profile switching.</p>
      </div>

      <div class="bento-card">
        <div class="bento-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
        </div>
        <h3>One-Tap Data Export</h3>
        <p>Your health data belongs to you. Export complete JSON backups and readable CSV spreadsheets anytime to analyze in Excel, Google Sheets, or Python.</p>
      </div>
    </div>
  </section>

  <!-- Scale Compatibility -->
  <section class="wrapper" id="compatibility">
    <div class="section-head">
      <h2>Cult Scale Compatibility Matrix</h2>
      <p>Tested and verified with the Cult / Fitkit hardware family.</p>
    </div>

    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>Model Name</th>
            <th>Connectivity</th>
            <th>Bioimpedance (BIA)</th>
            <th>WeightOCult Support</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><strong>Cult Smart Body Fat Scale</strong></td>
            <td>Bluetooth 5.0 (BLE)</td>
            <td>Stainless steel electrode pads</td>
            <td><span class="badge-check">✓ Native Bluetooth Sync</span></td>
          </tr>
          <tr>
            <td><strong>Cult Smart Scale Pro</strong></td>
            <td>Bluetooth 5.0 (BLE)</td>
            <td>Full ITO conductive glass</td>
            <td><span class="badge-check">✓ Native Bluetooth Sync</span></td>
          </tr>
          <tr>
            <td><strong>Cult Digital Body Weighing Scale</strong></td>
            <td>None (LCD Only)</td>
            <td>None</td>
            <td><span class="badge-manual">✎ Manual Entry Supported</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <!-- Open Source Banner -->
  <section class="wrapper">
    <div class="github-banner">
      <h2>100% Free & Open Source</h2>
      <p>Developed with Jetpack Compose & Kotlin. Maintained by <strong>techman395</strong> under the MIT License.</p>
      <div class="github-cta-group">
        <a href="https://github.com/techman395/weightocult" target="_blank" class="btn-github-large">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
          </svg>
          Star & Fork on GitHub
        </a>
        <a href="https://github.com/techman395/weightocult/releases/tag/v1.0.1" target="_blank" class="btn-secondary">
          View v1.0.1 Release Notes
        </a>
      </div>
    </div>
  </section>

  <!-- Footer -->
  <footer>
    <div class="wrapper footer-inner">
      <div>
        <strong>WeightOCult</strong> — Created & maintained by <a href="https://github.com/techman395" target="_blank" style="color: var(--cyan); text-decoration: none;">techman395</a>.
      </div>
      <div class="footer-links">
        <a href="https://github.com/techman395/weightocult" target="_blank">GitHub Repository</a>
        <a href="https://github.com/techman395/weightocult/blob/main/LICENSE" target="_blank">MIT License</a>
        <a href="https://github.com/bpepple/occult" target="_blank" rel="noopener">Upstream BLE Protocol Credits</a>
      </div>
    </div>
  </footer>

  <script>
    // Live subtle realistic numbers simulation
    let weight = 72.4;
    setInterval(() => {
      const delta = (Math.random() - 0.5) * 0.15;
      weight = Math.max(71.5, Math.min(73.5, weight + delta));
      document.getElementById('liveWeight').innerText = weight.toFixed(1);
    }, 2500);
  </script>
</body>
</html>`);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`WeightOCult server running on http://0.0.0.0:${PORT}`);
});
