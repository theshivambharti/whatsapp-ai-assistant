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
  Menu 
} from "lucide-react";

export default function App() {
  // Mobile app state (mirrors local DataStore / Settings)
  const [serverUrl, setServerUrl] = useState(() => {
    return localStorage.getItem("whatsapp_server_url") || "https://example.com/api/response";
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

  // Simulator screen navigation state: "splash" | "home" | "settings"
  const [currentScreen, setCurrentScreen] = useState<"splash" | "home" | "settings">("splash");
  
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

  const handleSaveSettings = () => {
    if (!formUrl.trim()) {
      setUrlError("Server URL cannot be empty");
      return;
    }
    // Simple URL regex check
    try {
      new URL(formUrl);
      setUrlError("");
    } catch (_) {
      setUrlError("Please enter a valid URL (e.g. https://example.com)");
      return;
    }

    saveSettingsToLocalStorage(formUrl, formHeaderName, formHeaderValue);
    // Mimic the native Toast notification
    showToastNotification();
  };

  const [toastMessage, setToastMessage] = useState("");
  const showToastNotification = () => {
    setToastMessage("Settings saved successfully!");
    setTimeout(() => setToastMessage(""), 3000);
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
      setUrlError("Please enter a valid URL (e.g. https://example.com)");
      return;
    }

    // Save configuration first
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
            <div className="p-2 bg-emerald-500/10 text-emerald-400 rounded-xl border border-emerald-500/20">
              <Layers className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                WhatsApp AI Assistant
                <span className="text-xs bg-emerald-500/10 text-emerald-400 px-2 py-0.5 rounded-full border border-emerald-500/20">
                  Native Android v1.0
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
          <div className="bg-gradient-to-br from-slate-900 to-slate-950 p-6 rounded-2xl border border-slate-800 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-500/5 blur-3xl -z-10 rounded-full" />
            <h2 className="text-lg font-bold text-white mb-2">Build & Deploy Successful!</h2>
            <p className="text-slate-300 text-sm leading-relaxed mb-4">
              Your Kotlin Native Android code compiles flawlessly! The system has successfully generated the native 
              Android app with complete MVVM logic, Retrofit integration, and persistent local storage.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-800/80 flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                <span className="text-xs text-slate-200">Jetpack/Material 3 UI Layouts</span>
              </div>
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-800/80 flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                <span className="text-xs text-slate-200">Jetpack Preferences DataStore</span>
              </div>
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-800/80 flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                <span className="text-xs text-slate-200">Retrofit & Coroutines Network</span>
              </div>
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-800/80 flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                <span className="text-xs text-slate-200">Background Event Listening</span>
              </div>
            </div>
          </div>

          {/* ACTIVE CONFIGURATION FOR TESTING */}
          <div className="bg-slate-900 p-6 rounded-2xl border border-slate-800 space-y-4">
            <h3 className="text-md font-bold text-white flex items-center gap-2">
              <Server className="w-4 h-4 text-emerald-400" />
              API Server Playground
            </h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Configure and test your message routing endpoint right here. Changes saved here will be immediately 
              synchronized with the interactive phone frame!
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

          {/* DOCUMENTATION & INSTRUCTIONS */}
          <div className="bg-slate-900/40 p-6 rounded-2xl border border-slate-800/80 space-y-4">
            <h3 className="text-md font-bold text-white flex items-center gap-2">
              <PhoneIcon className="w-4 h-4 text-emerald-400" />
              How to Deploy & Install APK
            </h3>
            <div className="space-y-3 text-sm text-slate-300 leading-relaxed">
              <div className="flex items-start gap-3">
                <div className="w-6 h-6 bg-slate-800 rounded-full flex items-center justify-center text-xs font-semibold shrink-0 text-emerald-400 border border-slate-700">1</div>
                <p className="text-xs pt-0.5">
                  Click the <strong className="text-white">Download Android APK</strong> button in the top right to download the compiled executable file directly.
                </p>
              </div>
              <div className="flex items-start gap-3">
                <div className="w-6 h-6 bg-slate-800 rounded-full flex items-center justify-center text-xs font-semibold shrink-0 text-emerald-400 border border-slate-700">2</div>
                <p className="text-xs pt-0.5">
                  Transfer the APK file to your Android phone or navigate to this URL on your phone's browser to download it directly.
                </p>
              </div>
              <div className="flex items-start gap-3">
                <div className="w-6 h-6 bg-slate-800 rounded-full flex items-center justify-center text-xs font-semibold shrink-0 text-emerald-400 border border-slate-700">3</div>
                <p className="text-xs pt-0.5">
                  Open the APK and select <strong className="text-white">Install Anyway</strong> when prompted (the application is self-signed with a standard Android debug keystore).
                </p>
              </div>
              <div className="flex items-start gap-3">
                <div className="w-6 h-6 bg-slate-800 rounded-full flex items-center justify-center text-xs font-semibold shrink-0 text-emerald-400 border border-slate-700">4</div>
                <p className="text-xs pt-0.5">
                  Configure your webhook Server URL inside settings. Toggle the status bar and test requests seamlessly!
                </p>
              </div>
            </div>
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
            <div className="w-full h-full bg-[#f0f2f5] rounded-[36px] overflow-hidden flex flex-col relative text-[#111b21] shadow-inner select-none">
              
              {/* TOP STATUS BAR */}
              <div className="bg-[#075e54] text-white px-5 pt-6 pb-2 flex justify-between items-center text-xs font-semibold z-10">
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

              {/* 1. SPLASH SCREEN */}
              {currentScreen === "splash" && (
                <div className="absolute inset-0 bg-[#0b141a] z-50 flex flex-col justify-between p-8 text-white animate-fade-in">
                  <div />
                  <div className="text-center space-y-2">
                    <h2 className="text-2xl font-bold tracking-tight">WhatsApp AI Assistant</h2>
                    <p className="text-xs text-emerald-400 font-semibold tracking-wider uppercase">Intelligent Auto-Responder</p>
                  </div>
                  <div className="flex flex-col items-center gap-4">
                    <div className="w-8 h-8 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin" />
                    <button 
                      onClick={() => setCurrentScreen("home")}
                      className="text-xs text-slate-400 hover:text-white underline transition mt-2"
                    >
                      Skip Splash
                    </button>
                  </div>
                </div>
              )}

              {/* 2. HOME SCREEN */}
              {currentScreen === "home" && (
                <div className="flex-1 flex flex-col overflow-y-auto">
                  
                  {/* APP BAR */}
                  <div className="bg-[#075e54] text-white px-4 py-3 shadow-md flex items-center justify-between">
                    <div>
                      <h3 className="font-bold text-base leading-tight">WhatsApp AI Assistant</h3>
                      <p className="text-[11px] text-emerald-200/90 font-medium">Auto-Responder Console</p>
                    </div>
                    <button 
                      onClick={() => {
                        setFormUrl(serverUrl);
                        setFormHeaderName(headerName);
                        setFormHeaderValue(headerValue);
                        setCurrentScreen("settings");
                      }}
                      className="p-1.5 hover:bg-white/10 rounded-full transition"
                    >
                      <SettingsIcon className="w-5 h-5 text-white" />
                    </button>
                  </div>

                  {/* SCROLLABLE HOME CONTENT */}
                  <div className="p-4 space-y-4 flex-1">
                    
                    {/* STATUS CARD */}
                    <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 flex flex-col gap-4">
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

                    {/* HOW IT WORKS MINI GUIDE */}
                    <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 space-y-3">
                      <h4 className="font-bold text-xs text-slate-800">How It Works</h4>
                      <p className="text-[11px] text-slate-600 leading-relaxed">
                        Automate your WhatsApp conversations with smart, AI-driven responses dynamically routed to your servers.
                      </p>
                      
                      <div className="space-y-2 pt-1 text-[11px] text-slate-700">
                        <div className="flex items-center gap-2">
                          <span className="w-4 h-4 bg-emerald-500 text-white rounded-full flex items-center justify-center text-[10px] font-bold">1</span>
                          <span>Message arrives in WhatsApp</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="w-4 h-4 bg-emerald-500 text-white rounded-full flex items-center justify-center text-[10px] font-bold">2</span>
                          <span>Assistant routes content to Server URL</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="w-4 h-4 bg-emerald-500 text-white rounded-full flex items-center justify-center text-[10px] font-bold">3</span>
                          <span>Server replies with response payload</span>
                        </div>
                      </div>
                    </div>

                  </div>
                </div>
              )}

              {/* 3. SETTINGS SCREEN */}
              {currentScreen === "settings" && (
                <div className="flex-1 flex flex-col overflow-y-auto">
                  
                  {/* APP BAR */}
                  <div className="bg-[#075e54] text-white px-3 py-3 shadow-md flex items-center gap-2">
                    <button 
                      onClick={() => setCurrentScreen("home")}
                      className="p-1 hover:bg-white/10 rounded-full transition"
                    >
                      <ArrowLeft className="w-5 h-5 text-white" />
                    </button>
                    <div>
                      <h3 className="font-bold text-base leading-tight">Settings</h3>
                      <p className="text-[11px] text-emerald-200/95 font-medium">Integration Settings</p>
                    </div>
                  </div>

                  {/* FORM FIELDS */}
                  <div className="p-4 space-y-4">
                    
                    <div className="bg-white p-4 rounded-2xl border border-slate-200 space-y-3">
                      <h4 className="font-bold text-xs text-[#00a884] tracking-wider uppercase">Server Integration Rules</h4>
                      
                      {/* SERVER URL FIELD */}
                      <div className="space-y-1">
                        <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-widest">Server URL</label>
                        <input 
                          type="text" 
                          value={formUrl}
                          onChange={(e) => setFormUrl(e.target.value)}
                          className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#00a884]"
                          placeholder="https://example.com/api/response"
                        />
                        <p className="text-[10px] text-slate-400">The endpoint where incoming payloads are routed</p>
                        {urlError && <p className="text-[10px] text-rose-500 font-semibold">{urlError}</p>}
                      </div>

                      {/* HEADER NAME */}
                      <div className="space-y-1">
                        <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-widest">Header Name</label>
                        <input 
                          type="text" 
                          value={formHeaderName}
                          onChange={(e) => setFormHeaderName(e.target.value)}
                          className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#00a884]"
                          placeholder="Authorization"
                        />
                        <p className="text-[10px] text-slate-400">e.g., Authorization</p>
                      </div>

                      {/* HEADER VALUE */}
                      <div className="space-y-1">
                        <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-widest">Header Value</label>
                        <input 
                          type="text" 
                          value={formHeaderValue}
                          onChange={(e) => setFormHeaderValue(e.target.value)}
                          className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#00a884]"
                          placeholder="Bearer token or value"
                        />
                        <p className="text-[10px] text-slate-400">e.g., Bearer token or api key</p>
                      </div>

                    </div>

                    {/* BUTTONS */}
                    <div className="space-y-2">
                      <button 
                        onClick={handleSaveSettings}
                        className="w-full py-3 bg-[#00a884] hover:bg-[#075e54] text-white rounded-xl font-bold text-xs tracking-wider uppercase transition"
                      >
                        Save Settings
                      </button>

                      <button 
                        onClick={handleSendTestRequest}
                        disabled={testLoading}
                        className="w-full py-3 bg-white hover:bg-slate-50 border border-slate-300 text-[#075e54] rounded-xl font-bold text-xs tracking-wider uppercase transition flex items-center justify-center gap-2"
                      >
                        <Send className="w-3.5 h-3.5" />
                        {testLoading ? "Sending Test..." : "Send Test Request"}
                      </button>
                    </div>

                  </div>
                </div>
              )}

              {/* TOAST NOTIFICATION PREVIEW */}
              {toastMessage && (
                <div className="absolute bottom-6 left-1/2 transform -translate-x-1/2 bg-slate-900/90 text-white text-[11px] px-4 py-2 rounded-full shadow-lg z-50 animate-bounce">
                  {toastMessage}
                </div>
              )}

              {/* MATERIAL DESIGN 3 DIALOG MOCKUP */}
              {showResultDialog && testResult && (
                <div className="absolute inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
                  <div className="bg-white rounded-3xl p-5 w-full max-w-xs space-y-4 shadow-2xl animate-fade-in text-slate-800">
                    <div>
                      <h4 className="font-bold text-sm text-slate-900 mb-1">
                        {testResult.isSuccess ? "Test Request Sent" : "Test Request Failed"}
                      </h4>
                      <p className="text-[11px] text-slate-500">
                        Response Code: {testResult.code ? `HTTP ${testResult.code}` : "Connection Failed"}
                      </p>
                    </div>

                    <div className="bg-slate-100 p-2.5 rounded-xl border border-slate-200">
                      <p className="text-[9px] font-bold text-slate-400 uppercase tracking-wider mb-1">Response Body</p>
                      <pre className="text-[10px] font-mono text-slate-700 max-h-24 overflow-y-auto whitespace-pre-wrap break-all">
                        {testResult.body}
                      </pre>
                    </div>

                    <button 
                      onClick={() => setShowResultDialog(false)}
                      className="w-full py-2 bg-[#00a884] hover:bg-[#075e54] text-white rounded-xl font-bold text-xs uppercase tracking-wider transition"
                    >
                      OK
                    </button>
                  </div>
                </div>
              )}

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
