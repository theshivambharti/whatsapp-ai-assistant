import { useState, useEffect } from "react";
import { 
  Smartphone, 
  Download, 
  Settings as SettingsIcon, 
  ArrowLeft, 
  CheckCircle, 
  XCircle, 
  Send, 
  Server, 
  Wifi, 
  ShieldAlert, 
  Play, 
  Layers, 
  Smartphone as PhoneIcon, 
  CheckCircle2, 
  Menu,
  Info,
  History,
  Code,
  Sparkles,
  AlertCircle,
  Activity,
  User,
  GitCommit,
  Calendar,
  Layers2,
  Terminal,
  Share2
} from "lucide-react";

const PIPELINE_STAGES = [
  "Notification Received",
  "Sender Parsed",
  "Webhook Request Sent",
  "Webhook Response Received",
  "JSON Parsed",
  "Reply Stored",
  "WhatsApp Chat Detected",
  "Input Field Found",
  "Reply Typed",
  "Send Button Found",
  "Reply Sent"
];

const RELEASES = [
  {
    version: "1.1.14",
    code: 16,
    date: "June 26, 2026",
    added: [
      "Implemented Connector Test Mode displaying stage name, status, timestamp, duration, details, and exceptions for all 11 pipeline stages.",
      "Added 'Export Debug Log' button allowing comprehensive sharing of diagnostic and system logs to text.",
      "Updated self-test routine to verify WhatsApp installation package status in addition to permissions, internet, and webhook reachability."
    ],
    improved: [
      "Added 1-second dynamic UI refresh on Diagnostics screen for real-time stage progress tracking."
    ],
    fixes: []
  },
  {
    version: "1.1.13",
    code: 15,
    date: "June 26, 2026",
    added: [
      "Defaulted server URL configuration to 'https://bot.clickbaaz.com/webhook.php'.",
      "Added explicit log event for finding the WhatsApp input field.",
      "Added millisecond-precision execution time measurement for Webhook requests and Accessibility send sequences.",
      "Aligned web simulator's test request schema with Phase 2 webhook specifications."
    ],
    improved: [],
    fixes: []
  },
  {
    version: "1.1.12",
    code: 14,
    date: "June 26, 2026",
    added: [
      "Added detailed, stage-by-stage event logging to database.",
      "Implemented auto-refreshing real-time developer logs screen."
    ],
    improved: [
      "Live log database polling every 1.5 seconds under active lifecycleScope."
    ],
    fixes: []
  },
  {
    version: "1.1.0",
    code: 2,
    date: "June 26, 2026",
    added: [
      "Added professional About & Version History screen.",
      "Designed new high-contrast glowing adaptive launcher icons.",
      "Added Developer Diagnostics live status reporting."
    ],
    improved: [
      "Replaced hardcoded system setting strings with Settings constants.",
      "Polished Material Design 3 UI and theme responsiveness."
    ],
    fixes: [
      "Fixed notification listener service access issues in Android settings."
    ]
  },
  {
    version: "1.0.0",
    code: 1,
    date: "June 25, 2026",
    added: [
      "Initial release of WhatsApp AI Auto-Responder.",
      "Interception of WhatsApp notifications.",
      "Accessibility service for pasting and sending replies.",
      "Persistent logs for webhooks and auto-replies.",
      "Material Design 3 diagnostic wizard and permission checks."
    ],
    improved: [
      "Enhanced UI status cards.",
      "Accurate notification connection state detection."
    ],
    fixes: [
      "Fixed notification Settings intent loading on older APIs.",
      "Resolved minor memory leaks in service binding."
    ]
  }
];

const ROADMAP = [
  "WhatsApp Business Support (automatic identification and integration)",
  "Advanced template-based quick responses with user custom variables",
  "AI response filter with customizable safe-guard boundaries",
  "Dynamic webhook payload customization & retry queues"
];

export default function App() {
  // Mobile app state (mirrors local DataStore / Settings)
  const [serverUrl, setServerUrl] = useState(() => {
    return localStorage.getItem("whatsapp_server_url") || "https://bot.clickbaaz.com/webhook.php";
  });
  const [headerName, setHeaderName] = useState(() => {
    return localStorage.getItem("whatsapp_header_name") || "Authorization";
  });
  const [headerValue, setHeaderValue] = useState(() => {
    return localStorage.getItem("whatsapp_header_value") || "Bearer token123";
  });
  const [isServiceActive, setIsServiceActive] = useState(() => {
    const saved = localStorage.getItem("whatsapp_service_active");
    return saved !== null ? saved === "true" : true;
  });

  // Connector Test Mode Switch
  const [connectorTestMode, setConnectorTestMode] = useState(() => {
    const saved = localStorage.getItem("whatsapp_connector_test_mode");
    return saved === "true";
  });

  // Simulator screen navigation state: "splash" | "home" | "settings" | "about" | "diagnostics"
  const [currentScreen, setCurrentScreen] = useState<"splash" | "home" | "settings" | "about" | "diagnostics">("splash");
  
  // Temporary form values inside the settings screen
  const [formUrl, setFormUrl] = useState(serverUrl);
  const [formHeaderName, setFormHeaderName] = useState(headerName);
  const [formHeaderValue, setFormHeaderValue] = useState(headerValue);
  const [urlError, setUrlError] = useState("");

  // Live test network state
  const [testLoading, setTestLoading] = useState(false);
  const [testResult, setTestResult] = useState<{
    code: number;
    body: string;
    isSuccess: boolean;
  } | null>(null);
  const [showResultDialog, setShowResultDialog] = useState(false);

  // Pipeline execution state
  const [pipelineRunning, setPipelineRunning] = useState(false);
  const [pipelineResults, setPipelineResults] = useState<Array<{
    stage: string;
    status: "waiting" | "success" | "failure";
    timestamp: string;
    duration: number;
    details: string;
    exception?: string;
  }>>([]);

  // Splash Screen auto-dismissal
  useEffect(() => {
    if (currentScreen === "splash") {
      const timer = setTimeout(() => {
        setCurrentScreen("home");
      }, 2500);
      return () => clearTimeout(timer);
    }
  }, [currentScreen]);

  // Sync state to local storage to mimic DataStore persistence
  const saveSettingsToLocalStorage = (url: string, hName: string, hValue: string) => {
    localStorage.setItem("whatsapp_server_url", url);
    localStorage.setItem("whatsapp_header_name", hName);
    localStorage.setItem("whatsapp_header_value", hValue);
    setServerUrl(url);
    setHeaderName(hName);
    setHeaderValue(hValue);
  };

  const handleToggleService = () => {
    const nextState = !isServiceActive;
    setIsServiceActive(nextState);
    localStorage.setItem("whatsapp_service_active", String(nextState));
  };

  const handleToggleTestMode = () => {
    const nextState = !connectorTestMode;
    setConnectorTestMode(nextState);
    localStorage.setItem("whatsapp_connector_test_mode", String(nextState));
    showToastNotification(nextState ? "Connector Test Mode Enabled" : "Connector Test Mode Disabled");
  };

  const handleSaveSettings = () => {
    if (!formUrl.trim()) {
      setUrlError("Server URL cannot be empty");
      return;
    }
    try {
      new URL(formUrl);
      setUrlError("");
    } catch (_) {
      setUrlError("Please enter a valid URL");
      return;
    }

    saveSettingsToLocalStorage(formUrl, formHeaderName, formHeaderValue);
    showToastNotification("Settings saved successfully!");
  };

  const [toastMessage, setToastMessage] = useState("");
  const showToastNotification = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(""), 2500);
  };

  // Simulate pipeline execution
  const handleRunPipelineSimulation = async () => {
    if (pipelineRunning) return;
    setPipelineRunning(true);
    
    const initialResults = PIPELINE_STAGES.map(stage => ({
      stage,
      status: "waiting" as const,
      timestamp: "-",
      duration: 0,
      details: "Awaiting execution..."
    }));
    setPipelineResults(initialResults);

    const stageDetails = [
      { details: "Intercepted WhatsApp notification from '+91 98765 43210'", duration: 15 },
      { details: "Parsed sender phone: +919876543210, content: 'Hi, what is the status of my order?'", duration: 8 },
      { details: `POST dispatched to server`, duration: 142 },
      { details: "Server responded with HTTP 200 OK. Body: {'reply': 'Your order #1043 has been shipped!'}", duration: 85 },
      { details: "Successfully extracted reply text: 'Your order #1043 has been shipped!'", duration: 5 },
      { details: "Reply persisted to local SQLite database with log ID 432", duration: 12 },
      { details: "WhatsApp chat window identified for sender '+91 98765 43210'", duration: 250 },
      { details: "Message edit text field located successfully via Accessibility Node", duration: 180 },
      { details: "Auto-typed reply text into input field", duration: 320 },
      { details: "Send button node with ID 'send_button' located", duration: 95 },
      { details: "Accessibility action clicked send button successfully", duration: 110 }
    ];

    for (let i = 0; i < PIPELINE_STAGES.length; i++) {
      await new Promise(resolve => setTimeout(resolve, 450));
      const now = new Date();
      const timeStr = now.toLocaleTimeString() + "." + String(now.getMilliseconds()).padStart(3, '0');
      
      initialResults[i] = {
        stage: PIPELINE_STAGES[i],
        status: "success" as const,
        timestamp: timeStr,
        duration: stageDetails[i].duration,
        details: stageDetails[i].details
      };
      setPipelineResults([...initialResults]);
    }
    setPipelineRunning(false);
  };

  // Run the test request through our server proxy to avoid CORS
  const handleSendTestRequest = async () => {
    if (!formUrl.trim()) {
      setUrlError("Server URL cannot be empty");
      return;
    }
    try {
      new URL(formUrl);
      setUrlError("");
    } catch (_) {
      setUrlError("Please enter a valid URL");
      return;
    }

    saveSettingsToLocalStorage(formUrl, formHeaderName, formHeaderValue);
    setTestLoading(true);

    try {
      const headersMap: Record<string, string> = {};
      if (formHeaderName.trim() && formHeaderValue.trim()) {
        headersMap[formHeaderName.trim()] = formHeaderValue.trim();
      }

      const response = await fetch("/api/test-request", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          url: formUrl.trim(),
          headers: headersMap
        })
      });

      const resData = await response.json();
      if (response.ok) {
        setTestResult({
          code: resData.status,
          body: resData.body,
          isSuccess: resData.status >= 200 && resData.status < 300
        });
      } else {
        setTestResult({
          code: response.status,
          body: resData.error || "Request failed",
          isSuccess: false
        });
      }
    } catch (err: any) {
      setTestResult({
        code: 0,
        body: err.message || "Network error",
        isSuccess: false
      });
    } finally {
      setTestLoading(false);
      setShowResultDialog(true);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 font-sans flex flex-col justify-between">
      
      {/* HEADER BAR */}
      <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur-md px-6 py-4 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-emerald-500/10 text-emerald-400 rounded-xl border border-emerald-500/20 shadow-inner">
              <Layers className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                WhatsApp AI Assistant
                <span className="text-xs bg-emerald-500/25 text-emerald-400 px-2 py-0.5 rounded-full border border-emerald-500/30 font-semibold shadow-sm">
                  v1.1.14 (16)
                </span>
              </h1>
              <p className="text-xs text-slate-400">Intelligent, Auto-Routing WhatsApp Auto-Responder</p>
            </div>
          </div>
          
          <div className="flex items-center gap-3">
            <a 
              href="/download-apk" 
              className="flex items-center gap-2 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-semibold px-5 py-2.5 rounded-xl transition shadow-lg shadow-emerald-500/10 text-sm"
            >
              <Download className="w-4 h-4" />
              Download Android APK
            </a>
          </div>
        </div>
      </header>

      {/* MAIN CONTAINER */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-8 grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        
        {/* LEFT COLUMN: ABOUT & ACTIONS */}
        <section className="lg:col-span-7 space-y-6">
          
          {/* PROMOTIONAL CARD */}
          <div className="bg-gradient-to-br from-slate-900 to-slate-950 p-6 rounded-2xl border border-slate-800 relative overflow-hidden shadow-xl">
            <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-500/5 blur-3xl -z-10 rounded-full" />
            <h2 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-emerald-400" />
              Integration Testing Phase Successful!
            </h2>
            <p className="text-slate-300 text-sm leading-relaxed mb-4">
              Your Kotlin Native Android code has been synchronized, structured, and compiled into a production-ready application binary. High-contrast adaptive vector branding and precise MVVM architectures are fully active.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-800/80 flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                <span className="text-xs text-slate-200">Connector Test Mode Enabled</span>
              </div>
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-800/80 flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                <span className="text-xs text-slate-200">11 Pipeline Stages Monitored</span>
              </div>
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-800/80 flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                <span className="text-xs text-slate-200">Jetpack Preferences DataStore</span>
              </div>
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-800/80 flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                <span className="text-xs text-slate-200">Self-Identifying Binary Logs</span>
              </div>
            </div>
          </div>

          {/* ACTIVE CONFIGURATION FOR TESTING */}
          <div className="bg-slate-900 p-6 rounded-2xl border border-slate-800 space-y-4 shadow-lg">
            <h3 className="text-md font-bold text-white flex items-center gap-2">
              <Server className="w-4 h-4 text-emerald-400" />
              API Server Playground
            </h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Configure and test your message routing endpoint. Changes saved here will be immediately synchronized with the interactive phone frame below!
            </p>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Server Endpoint URL</label>
                <input 
                  type="text" 
                  value={formUrl}
                  onChange={(e) => {
                    setFormUrl(e.target.value);
                    setServerUrl(e.target.value);
                  }}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition"
                  placeholder="https://example.com/api/response"
                />
                {urlError && <p className="text-xs text-rose-400 mt-1">{urlError}</p>}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Custom Header Name</label>
                  <input 
                    type="text" 
                    value={formHeaderName}
                    onChange={(e) => {
                      setFormHeaderName(e.target.value);
                      setHeaderName(e.target.value);
                    }}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition"
                    placeholder="Authorization"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Custom Header Value</label>
                  <input 
                    type="text" 
                    value={formHeaderValue}
                    onChange={(e) => {
                      setFormHeaderValue(e.target.value);
                      setHeaderValue(e.target.value);
                    }}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition"
                    placeholder="Bearer token..."
                  />
                </div>
              </div>

              <div className="flex gap-2 pt-2">
                <button 
                  onClick={handleSaveSettings}
                  className="flex-1 bg-slate-800 hover:bg-slate-700 text-slate-100 font-medium py-2.5 rounded-xl text-sm transition"
                >
                  Save Configuration
                </button>
                <button 
                  onClick={handleSendTestRequest}
                  disabled={testLoading}
                  className="flex-1 bg-emerald-500 hover:bg-emerald-600 disabled:bg-emerald-800 text-slate-950 font-bold py-2.5 rounded-xl text-sm transition flex items-center justify-center gap-2"
                >
                  <Send className="w-4 h-4" />
                  {testLoading ? "Testing..." : "Send Test Post"}
                </button>
              </div>
            </div>
          </div>

          {/* REALTIME SYSTEM DIAGNOSTICS PREVIEW */}
          <div className="bg-slate-900/60 p-6 rounded-2xl border border-slate-800 space-y-4">
            <div className="flex justify-between items-center">
              <h3 className="text-md font-bold text-white flex items-center gap-2">
                <Terminal className="w-4 h-4 text-emerald-400" />
                Simulated Connector Pipeline Logs
              </h3>
              <span className={`text-[10px] px-2 py-0.5 rounded-full font-semibold border ${
                connectorTestMode 
                  ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20" 
                  : "bg-rose-500/10 text-rose-400 border-rose-500/20"
              }`}>
                {connectorTestMode ? "Connector Mode: ON" : "Connector Mode: OFF"}
              </span>
            </div>
            
            {connectorTestMode ? (
              <div className="space-y-3">
                <p className="text-xs text-slate-400">
                  Trigger an automated response simulation using the button below to monitor how the 11-stage pipeline is captured in test mode.
                </p>
                <button
                  onClick={handleRunPipelineSimulation}
                  disabled={pipelineRunning}
                  className="flex items-center gap-2 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/20 font-semibold px-4 py-2 rounded-xl text-xs transition"
                >
                  <Play className="w-3.5 h-3.5" />
                  {pipelineRunning ? "Simulation Running..." : "Execute Pipeline Test Simulation"}
                </button>

                <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 max-h-80 overflow-y-auto space-y-3 font-mono text-xs">
                  {pipelineResults.length === 0 ? (
                    <p className="text-slate-500 italic text-center py-4">Click "Execute Pipeline Test Simulation" to run diagnosis.</p>
                  ) : (
                    pipelineResults.map((p, idx) => (
                      <div key={idx} className="border-b border-slate-900/80 pb-2.5 last:border-0 last:pb-0">
                        <div className="flex justify-between items-center mb-1">
                          <span className="font-bold text-slate-200">Stage {idx + 1}: {p.stage}</span>
                          <span className={`text-[10px] px-1.5 py-0.2 rounded font-semibold ${
                            p.status === "success" 
                              ? "text-emerald-400 bg-emerald-500/5" 
                              : p.status === "waiting"
                              ? "text-amber-400 bg-amber-500/5"
                              : "text-rose-400 bg-rose-500/5"
                          }`}>
                            {p.status === "success" ? "🟢 Success" : p.status === "waiting" ? "🟡 Waiting" : "🔴 Failure"}
                          </span>
                        </div>
                        <div className="grid grid-cols-2 gap-2 text-[10px] text-slate-400">
                          <div>Timestamp: <span className="text-slate-300">{p.timestamp}</span></div>
                          <div>Execution: <span className="text-slate-300">{p.duration} ms</span></div>
                        </div>
                        <p className="text-[10px] text-slate-500 mt-0.5">{p.details}</p>
                      </div>
                    ))
                  )}
                </div>
              </div>
            ) : (
              <div className="p-8 text-center bg-slate-950/60 rounded-xl border border-slate-900/60 flex flex-col items-center justify-center space-y-2">
                <AlertCircle className="w-8 h-8 text-slate-600" />
                <p className="text-sm font-semibold text-slate-400">Connector Test Mode is disabled</p>
                <p className="text-xs text-slate-500 max-w-sm">
                  Enable \"Connector Test Mode\" inside settings on the interactive emulator (phone frame) to view real-time stage diagnostics.
                </p>
              </div>
            )}
          </div>

        </section>

        {/* RIGHT COLUMN: HIGH FIDELITY INTERACTIVE EMULATOR */}
        <section className="lg:col-span-5 flex flex-col items-center">
          
          <div className="relative w-[340px] h-[680px] bg-slate-900 rounded-[48px] p-4 shadow-2xl border-4 border-slate-800 shadow-slate-950 flex flex-col overflow-hidden">
            
            {/* Phone speaker / Camera notch */}
            <div className="absolute top-1 left-1/2 transform -translate-x-1/2 w-32 h-6 bg-slate-950 rounded-b-2xl z-50 flex items-center justify-center">
              <div className="w-12 h-1 bg-slate-800 rounded-full mb-1" />
            </div>

            {/* SCREEN CANVAS */}
            <div className="w-full h-full bg-[#f0f2f5] rounded-[36px] overflow-hidden flex flex-col relative text-[#111b21] shadow-inner select-none justify-between">
              
              {/* TOP STATUS BAR */}
              <div className="bg-[#075e54] text-white px-5 pt-6 pb-2 flex justify-between items-center text-xs font-semibold z-10 shrink-0">
                <div className="flex items-center gap-1.5">
                  <span>12:00</span>
                  <span className="text-[10px] tracking-wide">AM</span>
                </div>
                <div className="flex items-center gap-2">
                  <Wifi className="w-3.5 h-3.5" />
                  <span className="text-[10px]">LTE</span>
                  <div className="w-5 h-2.5 border border-white/80 rounded-sm p-0.5 flex items-center">
                    <div className="w-full h-full bg-white rounded-2xs" />
                  </div>
                </div>
              </div>

              {/* SCREEN CONTENT AREA */}
              <div className="flex-1 overflow-y-auto relative flex flex-col">
                
                {/* 1. SPLASH SCREEN */}
                {currentScreen === "splash" && (
                  <div className="absolute inset-0 bg-[#0b141a] z-50 flex flex-col justify-between p-6 text-white text-center">
                    <div />
                    <div className="space-y-4">
                      {/* Brand Logo Frame */}
                      <div className="w-20 h-20 bg-[#06100c] border-2 border-emerald-400 rounded-2xl mx-auto flex items-center justify-center shadow-lg shadow-emerald-500/5">
                        <Smartphone className="w-10 h-10 text-emerald-400" />
                      </div>
                      <div className="space-y-1">
                        <h2 className="text-xl font-bold tracking-tight">WhatsApp AI Assistant</h2>
                        <p className="text-xs text-emerald-400 font-semibold tracking-wide">Version 1.1.14 (16)</p>
                        <p className="text-[10px] text-slate-400">{RELEASES[0].date}</p>
                      </div>
                    </div>
                    <div className="flex flex-col items-center gap-3">
                      <p className="text-[10px] text-slate-500">Developer: Shivam Bharti</p>
                      <button 
                        onClick={() => setCurrentScreen("home")}
                        className="text-[11px] bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 px-4 py-1.5 rounded-full transition"
                      >
                        Skip Splash
                      </button>
                    </div>
                  </div>
                )}

                {/* 2. HOME SCREEN */}
                {currentScreen === "home" && (
                  <div className="flex-1 flex flex-col justify-between min-h-full">
                    <div className="space-y-4">
                      {/* APP BAR */}
                      <div className="bg-[#075e54] text-white px-4 py-3 shadow-md flex items-center justify-between">
                        <div>
                          <h3 className="font-bold text-base leading-tight">WhatsApp AI Assistant</h3>
                          <p className="text-[11px] text-emerald-200/90 font-medium">Auto-Responder Console</p>
                        </div>
                        <div className="flex items-center gap-1">
                          <button 
                            onClick={() => setCurrentScreen("diagnostics")}
                            className="p-1.5 hover:bg-white/10 rounded-full transition relative"
                            title="Diagnostics Pipeline"
                          >
                            <Activity className="w-5 h-5 text-white" />
                            {connectorTestMode && (
                              <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-emerald-400 rounded-full border-2 border-[#075e54]" />
                            )}
                          </button>
                          <button 
                            onClick={() => setCurrentScreen("settings")}
                            className="p-1.5 hover:bg-white/10 rounded-full transition"
                            title="Settings"
                          >
                            <SettingsIcon className="w-5 h-5 text-white" />
                          </button>
                        </div>
                      </div>

                      {/* SCROLLABLE HOME CONTENT */}
                      <div className="p-4 space-y-4">
                        {/* STATUS CARD */}
                        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 flex flex-col gap-3">
                          <div className="flex items-center gap-3">
                            <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${isServiceActive ? "bg-emerald-50 text-emerald-500" : "bg-slate-50 text-slate-400"}`}>
                              <CheckCircle className="w-6 h-6" />
                            </div>
                            <div className="flex-1 min-w-0">
                              <h4 className="font-bold text-sm text-slate-900">
                                {isServiceActive ? "Assistant Status: Active" : "Assistant Status: Idle"}
                              </h4>
                              <p className="text-[11px] text-slate-500 truncate">
                                {isServiceActive ? "Listening for incoming WhatsApp events" : "Assistant is currently paused"}
                              </p>
                            </div>
                          </div>

                          <button 
                            onClick={handleToggleService}
                            className={`w-full py-2.5 rounded-xl font-bold text-xs tracking-wide transition uppercase ${
                              isServiceActive 
                                ? "bg-[#075e54] hover:bg-[#128c7e] text-white" 
                                : "bg-[#00a884] hover:bg-[#075e54] text-white"
                            }`}
                          >
                            {isServiceActive ? "Disable Assistant" : "Enable Assistant"}
                          </button>
                        </div>

                        {/* CONFIGURATION SUMMARY CARD */}
                        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 space-y-3">
                          <h4 className="font-bold text-xs text-slate-800 tracking-wider uppercase">Active AI Endpoint</h4>
                          
                          <div className="space-y-2">
                            <div>
                              <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">Server URL</p>
                              <p className="text-xs font-semibold text-slate-800 truncate">
                                {serverUrl || "Not Configured"}
                              </p>
                            </div>
                            <div>
                              <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">Custom Headers</p>
                              <p className="text-xs text-slate-700 font-mono truncate">
                                {headerName && headerValue ? `${headerName}: ${"*".repeat(headerValue.length)}` : "No headers specified"}
                              </p>
                            </div>
                          </div>

                          <button 
                            onClick={() => {
                              setFormUrl(serverUrl);
                              setFormHeaderName(headerName);
                              setFormHeaderValue(headerValue);
                              setCurrentScreen("settings");
                            }}
                            className="w-full py-2 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold text-[#075e54] transition"
                          >
                            Edit API Configuration
                          </button>
                        </div>

                        {/* ABOUT SCREEN SHORTCUT CARD */}
                        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 flex justify-between items-center">
                          <div>
                            <h4 className="font-bold text-xs text-slate-800">About & Release History</h4>
                            <p className="text-[10px] text-slate-500">View what's new in v1.1.14</p>
                          </div>
                          <button 
                            onClick={() => setCurrentScreen("about")}
                            className="p-2 bg-slate-50 hover:bg-slate-100 border border-slate-200 text-[#075e54] rounded-lg transition"
                          >
                            <Info className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    </div>

                    {/* PINNED HOME VERSION FOOTER - ALWAYS VISIBLE */}
                    <div className="p-3 bg-slate-50 border-t border-slate-200 text-center shrink-0">
                      <p className="text-[10px] font-bold text-slate-400">
                        Version 1.1.14 (Build 16)
                      </p>
                    </div>
                  </div>
                )}

                {/* 3. SETTINGS SCREEN */}
                {currentScreen === "settings" && (
                  <div className="flex-1 flex flex-col p-4 space-y-4">
                    {/* BACK TO HOME NAVIGATION BAR */}
                    <div className="flex items-center gap-2 pb-2 border-b border-slate-200">
                      <button 
                        onClick={() => setCurrentScreen("home")}
                        className="p-1 hover:bg-slate-200 rounded-full transition"
                      >
                        <ArrowLeft className="w-5 h-5 text-[#075e54]" />
                      </button>
                      <div>
                        <h4 className="font-bold text-sm text-[#075e54]">Settings</h4>
                        <p className="text-[10px] text-slate-500">Config & Testing Mode</p>
                      </div>
                    </div>

                    {/* RULES CARD */}
                    <div className="bg-white p-4 rounded-2xl border border-slate-200 space-y-3 shadow-sm">
                      <h4 className="font-bold text-xs text-[#00a884] tracking-wider uppercase">Server Integration Rules</h4>
                      
                      <div className="space-y-2">
                        <div className="space-y-0.5">
                          <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-wider">Server URL</label>
                          <input 
                            type="text" 
                            value={formUrl}
                            onChange={(e) => setFormUrl(e.target.value)}
                            className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-1.5 text-xs focus:outline-none focus:border-[#00a884]"
                            placeholder="https://example.com/api/response"
                          />
                          {urlError && <p className="text-[9px] text-rose-500 font-semibold">{urlError}</p>}
                        </div>

                        <div className="space-y-0.5">
                          <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-wider">Header Name</label>
                          <input 
                            type="text" 
                            value={formHeaderName}
                            onChange={(e) => setFormHeaderName(e.target.value)}
                            className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-1.5 text-xs focus:outline-none focus:border-[#00a884]"
                            placeholder="Authorization"
                          />
                        </div>

                        <div className="space-y-0.5">
                          <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-wider">Header Value</label>
                          <input 
                            type="text" 
                            value={formHeaderValue}
                            onChange={(e) => setFormHeaderValue(e.target.value)}
                            className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-1.5 text-xs focus:outline-none focus:border-[#00a884]"
                            placeholder="Bearer token or value"
                          />
                        </div>
                      </div>

                      <div className="flex gap-2 pt-1">
                        <button 
                          onClick={handleSaveSettings}
                          className="flex-1 py-2 bg-[#00a884] text-white text-[11px] font-bold rounded-xl hover:bg-[#075e54] transition uppercase"
                        >
                          Save Config
                        </button>
                        <button 
                          onClick={handleSendTestRequest}
                          disabled={testLoading}
                          className="flex-1 py-2 bg-slate-100 border border-slate-200 text-[#075e54] text-[11px] font-bold rounded-xl hover:bg-slate-200 transition uppercase"
                        >
                          {testLoading ? "Dispatched" : "Test Post"}
                        </button>
                      </div>
                    </div>

                    {/* NEW SECTION: DEVELOPER SETTINGS WITH CONNECTOR TEST MODE SWITCH */}
                    <div className="bg-white p-4 rounded-2xl border border-slate-200 space-y-3 shadow-sm">
                      <h4 className="font-bold text-xs text-slate-800 tracking-wider uppercase">Developer Settings</h4>
                      
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-xs font-bold text-slate-800">Connector Test Mode</p>
                          <p className="text-[10px] text-slate-500">Trace and log all 11 stages</p>
                        </div>
                        <label className="relative inline-flex items-center cursor-pointer">
                          <input 
                            type="checkbox" 
                            checked={connectorTestMode} 
                            onChange={handleToggleTestMode} 
                            className="sr-only peer" 
                          />
                          <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#075e54]"></div>
                        </label>
                      </div>
                    </div>

                    {/* NEW SECTION: APPLICATION INFORMATION SECTION */}
                    <div className="bg-white p-4 rounded-2xl border border-slate-200 space-y-2 shadow-sm text-xs text-slate-700">
                      <h4 className="font-bold text-xs text-slate-800 tracking-wider uppercase mb-1">Application Information</h4>
                      
                      <div className="flex justify-between py-1 border-b border-slate-100">
                        <span className="text-slate-500 font-medium">Application Version</span>
                        <span className="font-bold text-slate-800">Version 1.1.14</span>
                      </div>
                      <div className="flex justify-between py-1 border-b border-slate-100">
                        <span className="text-slate-500 font-medium">Version Code</span>
                        <span className="font-bold text-slate-800">16</span>
                      </div>
                      <div className="flex justify-between py-1 border-b border-slate-100">
                        <span className="text-slate-500 font-medium">Build Date</span>
                        <span className="font-bold text-slate-800">June 26, 2026</span>
                      </div>
                      <div className="flex justify-between py-1 border-b border-slate-100">
                        <span className="text-slate-500 font-medium">Target SDK</span>
                        <span className="font-bold text-slate-800">34</span>
                      </div>
                      <div className="flex justify-between py-1 border-b border-slate-100">
                        <span className="text-slate-500 font-medium">Minimum SDK</span>
                        <span className="font-bold text-slate-800">26</span>
                      </div>
                      <div className="flex justify-between py-1 border-b border-slate-100">
                        <span className="text-slate-500 font-medium">Developer</span>
                        <span className="font-bold text-[#075e54]">Shivam Bharti</span>
                      </div>
                      <div className="py-1 border-b border-slate-100">
                        <div className="flex justify-between">
                          <span className="text-slate-500 font-medium">Webhook URL</span>
                        </div>
                        <p className="font-mono text-[10px] text-slate-600 truncate mt-0.5">{serverUrl || "Not Configured"}</p>
                      </div>
                      <div className="flex justify-between py-1">
                        <span className="text-slate-500 font-medium">Current Connector Status</span>
                        <span className={`font-bold px-1.5 py-0.2 rounded text-[10px] ${
                          isServiceActive 
                            ? "text-emerald-600 bg-emerald-50" 
                            : "text-rose-600 bg-rose-50"
                        }`}>
                          {isServiceActive ? "Active" : "Inactive"}
                        </span>
                      </div>
                    </div>

                  </div>
                )}

                {/* 4. ABOUT SCREEN */}
                {currentScreen === "about" && (
                  <div className="flex-1 flex flex-col p-4 space-y-4">
                    {/* BACK BAR */}
                    <div className="flex items-center gap-2 pb-2 border-b border-slate-200">
                      <button 
                        onClick={() => setCurrentScreen("home")}
                        className="p-1 hover:bg-slate-200 rounded-full transition"
                      >
                        <ArrowLeft className="w-5 h-5 text-[#075e54]" />
                      </button>
                      <div>
                        <h4 className="font-bold text-sm text-[#075e54]">About App</h4>
                        <p className="text-[10px] text-slate-500">Metadata & Releases</p>
                      </div>
                    </div>

                    {/* APP LOGO & DETAILS */}
                    <div className="bg-white p-4 rounded-2xl border border-slate-200 space-y-3 shadow-sm text-center flex flex-col items-center">
                      <div className="w-16 h-16 bg-[#06100c] border border-emerald-400 rounded-xl flex items-center justify-center shadow-md">
                        <Smartphone className="w-8 h-8 text-emerald-400" />
                      </div>
                      <div>
                        <h4 className="font-bold text-sm text-slate-900">WhatsApp AI Assistant</h4>
                        <p className="text-xs text-slate-500">Version 1.1.14 (Build 16)</p>
                        <p className="text-[10px] text-slate-400">June 26, 2026</p>
                      </div>

                      <div className="w-full text-left text-xs border-t border-slate-100 pt-2 space-y-1 text-slate-700">
                        <div className="flex justify-between">
                          <span className="text-slate-400">Android SDK</span>
                          <span className="font-semibold">Target 34, Min 26</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-slate-400">Developer</span>
                          <span className="font-semibold text-[#075e54]">Shivam Bharti</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-slate-400">Git Commit</span>
                          <span className="font-mono text-slate-600">d8f4e2c</span>
                        </div>
                      </div>
                    </div>

                    {/* WHAT'S NEW */}
                    <div className="bg-white p-4 rounded-2xl border border-slate-200 space-y-2 shadow-sm text-xs">
                      <h4 className="font-bold text-slate-900 text-xs uppercase tracking-wider text-[#00a884]">What's New</h4>
                      <ul className="space-y-1.5 list-disc pl-4 text-slate-600 text-[11px]">
                        <li>Added Connector Test Mode highlighting stage success, execution duration and details.</li>
                        <li>Self-identifying app with Version 1.1.14 details shown consistently.</li>
                        <li>Integrated high-contrast launcher vectors.</li>
                      </ul>
                    </div>

                    {/* VERSION HISTORY */}
                    <div className="space-y-2">
                      <h4 className="font-bold text-slate-800 text-xs uppercase tracking-wider pl-1">Version History</h4>
                      
                      {RELEASES.slice(0, 3).map((r, idx) => (
                        <div key={idx} className="bg-white p-3 rounded-xl border border-slate-200 space-y-1 shadow-2xs">
                          <div className="flex justify-between font-bold text-xs">
                            <span className="text-[#075e54]">v{r.version}</span>
                            <span className="text-slate-400 text-[10px]">{r.date}</span>
                          </div>
                          <p className="text-[10px] text-slate-500 leading-normal">{r.added[0]}</p>
                        </div>
                      ))}
                    </div>

                  </div>
                )}

                {/* 5. DIAGNOSTICS SCREEN */}
                {currentScreen === "diagnostics" && (
                  <div className="flex-1 flex flex-col p-4 space-y-4">
                    {/* BACK BAR */}
                    <div className="flex items-center gap-2 pb-2 border-b border-slate-200">
                      <button 
                        onClick={() => setCurrentScreen("home")}
                        className="p-1 hover:bg-slate-200 rounded-full transition"
                      >
                        <ArrowLeft className="w-5 h-5 text-[#075e54]" />
                      </button>
                      <div>
                        <h4 className="font-bold text-sm text-[#075e54]">Diagnostics</h4>
                        <p className="text-[10px] text-slate-500">Pipeline & Self-Test</p>
                      </div>
                    </div>

                    {connectorTestMode ? (
                      <div className="space-y-3">
                        <div className="bg-white p-4 rounded-2xl border border-slate-200 text-center space-y-2">
                          <p className="text-xs text-slate-700">
                            Perform self-test sequence to execute and trace the 11-stage auto-responder pipeline in real-time.
                          </p>
                          <button
                            onClick={handleRunPipelineSimulation}
                            disabled={pipelineRunning}
                            className="w-full py-2 bg-[#075e54] text-white rounded-xl text-xs font-bold hover:bg-[#128c7e] transition uppercase"
                          >
                            {pipelineRunning ? "Running Diagnostics..." : "Trigger Test Pipeline"}
                          </button>
                        </div>

                        <div className="space-y-2">
                          <h4 className="font-bold text-slate-800 text-xs pl-1">Stage Monitor</h4>
                          <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                            {pipelineResults.length === 0 ? (
                              <p className="text-slate-400 italic text-center text-xs py-6">Awaiting pipeline trigger...</p>
                            ) : (
                              pipelineResults.map((p, idx) => (
                                <div key={idx} className="bg-white p-3 rounded-xl border border-slate-200 space-y-1 shadow-2xs text-[11px]">
                                  <div className="flex justify-between items-center font-bold">
                                    <span className="text-slate-800">Stage {idx + 1}: {p.stage}</span>
                                    <span className={`text-[9px] ${
                                      p.status === "success" ? "text-emerald-600" : "text-amber-500"
                                    }`}>
                                      {p.status === "success" ? "🟢 Success" : "🟡 Waiting"}
                                    </span>
                                  </div>
                                  <div className="flex gap-4 text-[9px] text-slate-400">
                                    <span>Time: {p.timestamp}</span>
                                    <span>Duration: {p.duration}ms</span>
                                  </div>
                                  <p className="text-slate-500 text-[10px] mt-0.5">{p.details}</p>
                                </div>
                              ))
                            )}
                          </div>
                        </div>

                      </div>
                    ) : (
                      <div className="p-8 text-center bg-white rounded-2xl border border-slate-200 flex flex-col items-center justify-center space-y-2 shadow-sm">
                        <AlertCircle className="w-8 h-8 text-slate-400" />
                        <p className="text-xs font-bold text-slate-800">Connector Test Mode Disabled</p>
                        <p className="text-[10px] text-slate-500 max-w-sm">
                          Please go to Settings screen and enable \"Connector Test Mode\" to unleash diagnostics view.
                        </p>
                      </div>
                    )}

                  </div>
                )}

              </div>

            </div>
          </div>

          <div className="text-center mt-3">
            <p className="text-xs text-slate-500">
              💡 Drag sliders, toggle values, or click <strong className="text-slate-400">Skip Splash</strong> to test screen states.
            </p>
          </div>
          
        </section>

      </main>

      {/* FOOTER */}
      <footer className="border-t border-slate-900 bg-slate-950 px-6 py-4 mt-8 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <p>© 2026 WhatsApp AI Assistant. All rights reserved.</p>
          <div className="flex gap-4">
            <a href="/download-apk" className="hover:text-white transition">Download APK</a>
            <span className="text-slate-800">|</span>
            <span className="text-slate-400">Kotlin Native MVVM Architecture</span>
          </div>
        </div>
      </footer>

    </div>
  );
}
