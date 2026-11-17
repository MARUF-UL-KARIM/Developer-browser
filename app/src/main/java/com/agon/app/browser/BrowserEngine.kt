package com.agon.app.browser

object BrowserEngine {

    const val DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/142.0.0.0 Safari/537.36"

    const val BRIDGE = "__devbridge"

    /** Host fragments blocked by the built-in EasyList-style content blocker. */
    val blockedHosts = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "analytics.google.com", "adservice.google.com",
        "facebook.net/en_US/fbevents", "connect.facebook.net", "scorecardresearch.com",
        "adnxs.com", "criteo.com", "taboola.com", "outbrain.com", "pubmatic.com",
        "rubiconproject.com", "openx.net", "moatads.com", "amazon-adsystem.com",
        "hotjar.com", "mixpanel.com", "segment.io", "branch.io", "adsrvr.org",
        "zedo.com", "quantserve.com", "bluekai.com", "sharethrough.com",
    )

    fun isBlocked(url: String): Boolean {
        val lower = url.lowercase()
        return blockedHosts.any { lower.contains(it) }
    }

    fun resourceTypeOf(url: String, accept: String?): String {
        val clean = url.substringBefore('?').lowercase()
        return when {
            accept?.contains("text/html") == true -> "doc"
            clean.endsWith(".js") || clean.endsWith(".mjs") -> "js"
            clean.endsWith(".css") -> "css"
            clean.endsWith(".png") || clean.endsWith(".jpg") || clean.endsWith(".jpeg") ||
                clean.endsWith(".gif") || clean.endsWith(".webp") || clean.endsWith(".svg") ||
                clean.endsWith(".ico") || clean.endsWith(".avif") -> "img"
            clean.endsWith(".woff") || clean.endsWith(".woff2") || clean.endsWith(".ttf") ||
                clean.endsWith(".otf") -> "font"
            clean.endsWith(".json") -> "xhr"
            clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".m3u8") ||
                clean.endsWith(".mp3") || clean.endsWith(".m4a") -> "media"
            clean.endsWith(".html") || clean.endsWith("/") -> "doc"
            else -> "other"
        }
    }

    /** Injected at document start of every page: console bridge, error capture, DevTools API. */
    val devtoolsRuntime: String = """
(function(){
  if (window.__devbrowser_ready) return;
  window.__devbrowser_ready = true;
  var B = window.__devbridge;
  function ser(a){
    try {
      if (typeof a === 'string') return a;
      if (a === undefined) return 'undefined';
      if (a === null) return 'null';
      if (a instanceof Error) return (a.stack || (a.name + ': ' + a.message));
      if (a && a.nodeType === 1) return '<' + a.nodeName.toLowerCase() + (a.id ? ' id="' + a.id + '"' : '') + '>';
      if (typeof a === 'function') return 'function ' + (a.name || 'anonymous') + '()';
      var seen = [];
      return JSON.stringify(a, function(k, v){
        if (typeof v === 'object' && v !== null) {
          if (seen.indexOf(v) >= 0) return '[Circular]';
          seen.push(v);
        }
        if (typeof v === 'function') return 'function ' + (v.name || 'anonymous') + '()';
        return v;
      }, 2);
    } catch (e) { return String(a); }
  }
  window.__db_ser = ser;
  function emit(level, args){
    try { B.consoleMessage(level, Array.prototype.map.call(args, ser).join(' ')); } catch (e) {}
  }
  var native = {};
  ['log','info','warn','error','debug'].forEach(function(lvl){
    native[lvl] = console[lvl] ? console[lvl].bind(console) : function(){};
    console[lvl] = function(){ emit(lvl, arguments); native[lvl].apply(console, arguments); };
  });
  var counters = {}, timers = {}, groupDepth = 0;
  console.count = function(l){ l = l || 'default'; counters[l] = (counters[l] || 0) + 1; emit('info', [l + ': ' + counters[l]]); };
  console.countReset = function(l){ counters[l || 'default'] = 0; };
  console.time = function(l){ timers[l || 'default'] = performance.now(); };
  console.timeEnd = function(l){
    l = l || 'default';
    if (timers[l] != null) { emit('info', [l + ': ' + (performance.now() - timers[l]).toFixed(2) + ' ms']); delete timers[l]; }
    else emit('warn', ['Timer "' + l + '" does not exist']);
  };
  console.table = function(d){ emit('info', [d]); };
  console.group = function(){ groupDepth++; emit('group', arguments); };
  console.groupCollapsed = console.group;
  console.groupEnd = function(){ if (groupDepth > 0) groupDepth--; emit('groupEnd', ['']); };
  console.assert = function(c){ if (!c) emit('error', ['Assertion failed']); };
  console.trace = function(){ try { throw new Error('trace'); } catch (e) { emit('debug', [e.stack]); } };
  console.clear = function(){ try { B.consoleClear(); } catch (e) {} };
  window.addEventListener('error', function(e){
    emit('error', [(e.message || 'Script error') + '\n    at ' + (e.filename || 'unknown') + ':' + (e.lineno || 0) + ':' + (e.colno || 0)]);
  }, true);
  window.addEventListener('unhandledrejection', function(e){
    emit('error', ['Uncaught (in promise) ' + ser(e.reason)]);
  });

  /* ---------- DOM inspection ---------- */
  function resolve(path){
    var el = document.documentElement;
    if (!path) return el;
    var parts = path.split('.');
    for (var i = 0; i < parts.length; i++) {
      if (!el) return null;
      el = el.children[parseInt(parts[i], 10)];
    }
    return el || null;
  }
  window.__db_resolve = resolve;
  function attrs(el){
    var o = {};
    for (var i = 0; i < el.attributes.length; i++) o[el.attributes[i].name] = el.attributes[i].value;
    return o;
  }
  function brief(el, path){
    return {
      path: path,
      tag: el.tagName ? el.tagName.toLowerCase() : '#node',
      id: el.id || '',
      cls: (typeof el.className === 'string' ? el.className : ''),
      text: el.children.length === 0 ? (el.textContent || '').trim().substring(0, 120) : '',
      kids: el.children.length,
      attrs: attrs(el)
    };
  }
  window.__db_root = function(){ return JSON.stringify([brief(document.documentElement, '')]); };
  window.__db_children = function(path){
    var el = resolve(path);
    if (!el) return '[]';
    var out = [];
    for (var i = 0; i < el.children.length && i < 300; i++) {
      out.push(brief(el.children[i], path === '' ? String(i) : path + '.' + i));
    }
    return JSON.stringify(out);
  };
  window.__db_styles = function(path){
    var el = resolve(path);
    if (!el) return '{}';
    var cs = getComputedStyle(el);
    var r = el.getBoundingClientRect();
    var props = ['display','position','width','height','color','background-color','font-family','font-size','font-weight',
      'margin','padding','border','border-radius','flex-direction','justify-content','align-items','gap','z-index',
      'opacity','overflow','text-align','line-height','box-shadow','transform'];
    var computed = {};
    props.forEach(function(p){ computed[p] = cs.getPropertyValue(p); });
    var inline = {};
    for (var i = 0; i < el.style.length; i++) inline[el.style[i]] = el.style.getPropertyValue(el.style[i]);
    var matched = [];
    try {
      for (var s = 0; s < document.styleSheets.length && matched.length < 40; s++) {
        var rules = null;
        try { rules = document.styleSheets[s].cssRules; } catch (e) { continue; }
        if (!rules) continue;
        for (var k = 0; k < rules.length && matched.length < 40; k++) {
          var rule = rules[k];
          if (rule.selectorText) {
            try { if (el.matches(rule.selectorText)) matched.push(rule.cssText.substring(0, 400)); } catch (e) {}
          }
        }
      }
    } catch (e) {}
    return JSON.stringify({
      computed: computed,
      inline: inline,
      matched: matched,
      box: {
        width: Math.round(r.width), height: Math.round(r.height),
        top: Math.round(r.top), left: Math.round(r.left),
        marginTop: cs.marginTop, marginRight: cs.marginRight, marginBottom: cs.marginBottom, marginLeft: cs.marginLeft,
        paddingTop: cs.paddingTop, paddingRight: cs.paddingRight, paddingBottom: cs.paddingBottom, paddingLeft: cs.paddingLeft,
        borderTop: cs.borderTopWidth, borderRight: cs.borderRightWidth, borderBottom: cs.borderBottomWidth, borderLeft: cs.borderLeftWidth
      }
    });
  };
  window.__db_highlight = function(path){
    var old = document.getElementById('__db_hl');
    if (old) old.remove();
    var el = resolve(path);
    if (!el) return 'false';
    var r = el.getBoundingClientRect();
    var d = document.createElement('div');
    d.id = '__db_hl';
    d.style.cssText = 'position:fixed;pointer-events:none;z-index:2147483647;border:2px solid #1B6EF3;' +
      'background:rgba(27,110,243,.18);transition:all .12s ease;' +
      'top:' + r.top + 'px;left:' + r.left + 'px;width:' + r.width + 'px;height:' + r.height + 'px;';
    document.body.appendChild(d);
    el.scrollIntoView({block:'center', behavior:'smooth'});
    setTimeout(function(){ var n = document.getElementById('__db_hl'); if (n) n.remove(); }, 2200);
    return 'true';
  };
  window.__db_setattr = function(path, name, value){
    var el = resolve(path); if (!el) return 'false';
    if (value === null || value === '') el.removeAttribute(name); else el.setAttribute(name, value);
    return 'true';
  };
  window.__db_settext = function(path, value){
    var el = resolve(path); if (!el) return 'false'; el.textContent = value; return 'true';
  };
  window.__db_sethtml = function(path, value){
    var el = resolve(path); if (!el) return 'false'; el.outerHTML = value; return 'true';
  };
  window.__db_setstyle = function(path, prop, value){
    var el = resolve(path); if (!el) return 'false'; el.style.setProperty(prop, value); return 'true';
  };
  window.__db_delete = function(path){
    var el = resolve(path); if (!el || el === document.documentElement) return 'false'; el.remove(); return 'true';
  };
  window.__db_search = function(q){
    var out = [];
    try {
      var nodes = document.querySelectorAll(q);
      for (var i = 0; i < nodes.length && i < 50; i++) {
        var el = nodes[i], path = [], cur = el;
        while (cur && cur !== document.documentElement) {
          var p = cur.parentElement; if (!p) break;
          path.unshift(Array.prototype.indexOf.call(p.children, cur)); cur = p;
        }
        out.push(brief(el, path.join('.')));
      }
    } catch (e) { return JSON.stringify({error: String(e)}); }
    return JSON.stringify(out);
  };
  window.__db_inspect_mode = function(on){
    if (on) {
      window.__db_pick = function(ev){
        ev.preventDefault(); ev.stopPropagation();
        var el = ev.target, path = [], cur = el;
        while (cur && cur !== document.documentElement) {
          var p = cur.parentElement; if (!p) break;
          path.unshift(Array.prototype.indexOf.call(p.children, cur)); cur = p;
        }
        try { B.elementPicked(path.join('.')); } catch (e) {}
        window.__db_inspect_mode(false);
      };
      document.addEventListener('click', window.__db_pick, true);
      document.body.style.cursor = 'crosshair';
    } else if (window.__db_pick) {
      document.removeEventListener('click', window.__db_pick, true);
      document.body.style.cursor = '';
    }
    return 'ok';
  };

  /* ---------- Storage ---------- */
  window.__db_storage = function(){
    var out = {local: [], session: [], cookies: []};
    try { for (var i = 0; i < localStorage.length; i++) { var k = localStorage.key(i); out.local.push([k, localStorage.getItem(k)]); } } catch (e) {}
    try { for (var j = 0; j < sessionStorage.length; j++) { var s = sessionStorage.key(j); out.session.push([s, sessionStorage.getItem(s)]); } } catch (e) {}
    try {
      document.cookie.split(';').forEach(function(c){
        if (!c.trim()) return;
        var idx = c.indexOf('=');
        out.cookies.push([c.substring(0, idx).trim(), c.substring(idx + 1)]);
      });
    } catch (e) {}
    out.dbs = [];
    return JSON.stringify(out);
  };
  window.__db_storage_set = function(area, k, v){
    try { (area === 'session' ? sessionStorage : localStorage).setItem(k, v); return 'true'; } catch (e) { return 'false'; }
  };
  window.__db_storage_del = function(area, k){
    try {
      if (area === 'cookies') { document.cookie = k + '=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'; return 'true'; }
      (area === 'session' ? sessionStorage : localStorage).removeItem(k); return 'true';
    } catch (e) { return 'false'; }
  };
  window.__db_storage_clear = function(area){
    try {
      if (area === 'local') localStorage.clear();
      else if (area === 'session') sessionStorage.clear();
      else if (area === 'cookies') document.cookie.split(';').forEach(function(c){
        document.cookie = c.split('=')[0].trim() + '=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
      });
      return 'true';
    } catch (e) { return 'false'; }
  };

  /* ---------- Performance ---------- */
  window.__db_perf = function(){
    var out = {};
    try {
      var nav = performance.getEntriesByType('navigation')[0];
      if (nav) {
        out.dns = Math.round(nav.domainLookupEnd - nav.domainLookupStart);
        out.connect = Math.round(nav.connectEnd - nav.connectStart);
        out.ttfb = Math.round(nav.responseStart - nav.requestStart);
        out.download = Math.round(nav.responseEnd - nav.responseStart);
        out.domInteractive = Math.round(nav.domInteractive);
        out.domComplete = Math.round(nav.domComplete);
        out.loadEvent = Math.round(nav.loadEventEnd);
        out.transferSize = nav.transferSize || 0;
        out.protocol = nav.nextHopProtocol || 'unknown';
        out.type = nav.type;
      }
      var paints = performance.getEntriesByType('paint');
      paints.forEach(function(p){
        if (p.name === 'first-paint') out.firstPaint = Math.round(p.startTime);
        if (p.name === 'first-contentful-paint') out.fcp = Math.round(p.startTime);
      });
      var res = performance.getEntriesByType('resource');
      out.resourceCount = res.length;
      var bytes = 0, byType = {};
      res.forEach(function(r){
        bytes += (r.transferSize || 0);
        byType[r.initiatorType] = (byType[r.initiatorType] || 0) + 1;
      });
      out.resourceBytes = bytes;
      out.byType = byType;
      out.domNodes = document.getElementsByTagName('*').length;
      out.scripts = document.scripts.length;
      out.stylesheets = document.styleSheets.length;
      out.images = document.images.length;
      out.iframes = document.getElementsByTagName('iframe').length;
      out.listeners = -1;
      if (performance.memory) {
        out.heapUsed = performance.memory.usedJSHeapSize;
        out.heapTotal = performance.memory.totalJSHeapSize;
        out.heapLimit = performance.memory.jsHeapSizeLimit;
      }
      var longTasks = performance.getEntriesByType('longtask');
      out.longTasks = longTasks ? longTasks.length : 0;
    } catch (e) { out.error = String(e); }
    return JSON.stringify(out);
  };
  window.__db_fps = function(){
    var frames = 0, start = performance.now();
    function tick(){
      frames++;
      if (performance.now() - start < 1000) requestAnimationFrame(tick);
      else { try { B.fpsResult(Math.round(frames * 1000 / (performance.now() - start))); } catch (e) {} }
    }
    requestAnimationFrame(tick);
    return 'measuring';
  };
  window.__db_resources = function(){
    var out = [];
    try {
      performance.getEntriesByType('resource').forEach(function(r){
        out.push({url: r.name, type: r.initiatorType, dur: Math.round(r.duration),
          size: r.transferSize || r.encodedBodySize || 0, start: Math.round(r.startTime),
          proto: r.nextHopProtocol || '', cached: (r.transferSize === 0 && r.decodedBodySize > 0)});
      });
    } catch (e) {}
    return JSON.stringify(out);
  };
  window.__db_report_resources = function(){
    try { B.resourceTimings(window.__db_resources()); } catch (e) {}
  };

  /* ---------- Sources / assets ---------- */
  window.__db_source = function(){ return document.documentElement.outerHTML; };
  window.__db_assets = function(){
    var out = [];
    function push(u, t){ if (u && u.indexOf('data:') !== 0) out.push({url: u, type: t}); }
    Array.prototype.forEach.call(document.images, function(i){ push(i.currentSrc || i.src, 'image'); });
    Array.prototype.forEach.call(document.scripts, function(s){ push(s.src, 'script'); });
    Array.prototype.forEach.call(document.querySelectorAll('link[rel=stylesheet]'), function(l){ push(l.href, 'stylesheet'); });
    Array.prototype.forEach.call(document.querySelectorAll('video,audio,source'), function(v){ push(v.currentSrc || v.src, 'media'); });
    Array.prototype.forEach.call(document.querySelectorAll('a[href]'), function(a){
      var h = a.href.split('?')[0].toLowerCase();
      if (/\.(pdf|zip|apk|docx?|xlsx?|pptx?|mp3|mp4|rar|7z|csv|txt|json)${'$'}/.test(h)) push(a.href, 'file');
    });
    var seen = {}, uniq = [];
    out.forEach(function(o){ if (!seen[o.url]) { seen[o.url] = 1; uniq.push(o); } });
    return JSON.stringify(uniq.slice(0, 200));
  };
  window.__db_readable = function(){
    var best = null, bestScore = 0;
    var cands = document.querySelectorAll('article, main, [role=main], .post, .content, #content, body');
    Array.prototype.forEach.call(cands, function(c){
      var ps = c.querySelectorAll('p');
      var score = 0;
      Array.prototype.forEach.call(ps, function(p){ score += (p.textContent || '').length; });
      if (score > bestScore) { bestScore = score; best = c; }
    });
    if (!best) return JSON.stringify({title: document.title, html: '', text: ''});
    var clone = best.cloneNode(true);
    Array.prototype.forEach.call(clone.querySelectorAll('script,style,nav,footer,aside,form,iframe,noscript'), function(n){ n.remove(); });
    return JSON.stringify({title: document.title, html: clone.innerHTML, text: (clone.textContent || '').replace(/\s+/g, ' ').trim()});
  };
  window.__db_text = function(){ return (document.body ? document.body.innerText : '').substring(0, 60000); };
  window.__db_toggle_images = function(on){
    Array.prototype.forEach.call(document.images, function(i){ i.style.visibility = on ? 'visible' : 'hidden'; });
    return 'ok';
  };
  window.__db_meta = function(){
    var out = {title: document.title, url: location.href, charset: document.characterSet,
      lang: document.documentElement.lang || '(not set)', referrer: document.referrer || '(none)',
      cookieEnabled: navigator.cookieEnabled, online: navigator.onLine, ua: navigator.userAgent,
      viewport: window.innerWidth + ' x ' + window.innerHeight, dpr: window.devicePixelRatio,
      serviceWorkers: 0, metas: []};
    try {
      Array.prototype.forEach.call(document.querySelectorAll('meta[name], meta[property]'), function(m){
        out.metas.push([(m.getAttribute('name') || m.getAttribute('property')), (m.getAttribute('content') || '').substring(0, 200)]);
      });
    } catch (e) {}
    return JSON.stringify(out);
  };
})();
"""

    const val DO_NOT_TRACK_JS: String =
        "(function(){try{Object.defineProperty(navigator,'doNotTrack',{get:function(){return '1';}," +
            "configurable:true});Object.defineProperty(navigator,'globalPrivacyControl'," +
            "{get:function(){return true;},configurable:true});}catch(e){}})();"

    fun wrapEval(expression: String): String = """
(function(){
  try {
    var __r = eval(${jsStringLiteral(expression)});
    return (window.__db_ser ? window.__db_ser(__r) : String(__r));
  } catch (e) {
    return 'Uncaught ' + (e && e.stack ? e.stack : String(e));
  }
})()
"""

    fun jsStringLiteral(value: String): String {
        val sb = StringBuilder("\"")
        for (c in value) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    fun userScriptWrapper(name: String, code: String): String =
        "(function(){try{\n$code\n}catch(e){console.error('[UserScript ${name.replace("'", "")}] ' + e);}})();"

    fun readerCss(dark: Boolean): String {
        val bg = if (dark) "#10131a" else "#fbfaf7"
        val fg = if (dark) "#e4e7ee" else "#1a1d22"
        val accent = if (dark) "#9ec2ff" else "#1b6ef3"
        return "body{background:$bg!important;color:$fg!important;font:18px/1.65 -apple-system,Roboto,sans-serif!important;" +
            "max-width:760px;margin:0 auto!important;padding:24px 20px 64px!important;}" +
            "h1,h2,h3{color:$fg!important;line-height:1.25!important}a{color:$accent!important}" +
            "img,video{max-width:100%!important;height:auto!important;border-radius:10px}" +
            "pre,code{background:rgba(127,127,127,.15)!important;border-radius:6px;padding:2px 6px;font-size:15px}"
    }

    fun formatJson(input: String): String = try {
        val element = kotlinx.serialization.json.Json.parseToJsonElement(input.trim())
        kotlinx.serialization.json.Json { prettyPrint = true }.encodeToString(
            kotlinx.serialization.json.JsonElement.serializer(), element,
        )
    } catch (e: Exception) {
        "Invalid JSON: ${e.message}"
    }

    fun formatMarkup(input: String): String {
        val sb = StringBuilder()
        var indent = 0
        val tokens = input.replace(">\\s*<".toRegex(), ">\n<").lines()
        for (raw in tokens) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("</")) indent = (indent - 1).coerceAtLeast(0)
            sb.append("  ".repeat(indent)).append(line).append('\n')
            val selfClosing = line.endsWith("/>") || line.startsWith("<!") ||
                line.startsWith("<meta") || line.startsWith("<link") || line.startsWith("<br") ||
                line.startsWith("<img") || line.startsWith("<input")
            val hasClose = line.contains("</") && !line.startsWith("</")
            if (line.startsWith("<") && !line.startsWith("</") && !selfClosing && !hasClose) indent++
        }
        return sb.toString()
    }

    fun formatCss(input: String): String {
        val sb = StringBuilder()
        var indent = 0
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when (c) {
                '{' -> { indent++; sb.append(" {\n").append("  ".repeat(indent)) }
                '}' -> { indent = (indent - 1).coerceAtLeast(0); sb.append("\n").append("  ".repeat(indent)).append("}\n").append("  ".repeat(indent)) }
                ';' -> sb.append(";\n").append("  ".repeat(indent))
                '\n', '\r' -> {}
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString().lines().filter { it.isNotBlank() }.joinToString("\n")
    }

    fun prettySize(bytes: Long): String = when {
        bytes < 0 -> "\u2014"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f kB", bytes / 1024.0)
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
