"use strict";
// // Saves options to chrome.storage
// function save_options()
// {
//     var color = document.getElementById('color').value;
//     var likesColor = document.getElementById('like').checked;
//     chrome.storage.sync.set({
//         favoriteColor: color,
//         likesColor: likesColor
//     }, function ()
//     {
//         // Update status to let user know options were saved.
//         var status = document.getElementById('status');
//         status.textContent = 'Options saved.';
//         setTimeout(function ()
//         {
//             status.textContent = '';
//         }, 750);
//     });
// }
var inputDepth;
var inputThreads;
var inputShowHints;
var inputMoveAnalysis;
var inputDepthBar;
var inputEvalBar;
var inputAutoMove;
var inputUseNNUE;
const DefaultExtensionOptions = {
    depth: 15,
    threads: 2,
    show_hints: true,
    move_analysis: true,
    depth_bar: true,
    evaluation_bar: true,
    use_nnue: false,
    auto_move: false,
};
function RestoreOptions() {
    chrome.storage.sync.get(DefaultExtensionOptions, function (opts) {
        let options = opts;
        inputDepth.value = options.depth.toString();
        inputThreads.value = options.threads.toString();
        inputShowHints.checked = options.show_hints;
        inputMoveAnalysis.checked = options.move_analysis;
        inputDepthBar.checked = options.depth_bar;
        inputEvalBar.checked = options.evaluation_bar;
        inputUseNNUE.checked = options.use_nnue;
        inputAutoMove.checked = options.auto_move;
        let event = new CustomEvent("input");
        event.disableUpdate = true;
        inputDepth.dispatchEvent(event);
        inputThreads.dispatchEvent(event);
    });
}
function OnOptionsChange() {
    let options = {
        depth: parseInt(inputDepth.value),
        threads: parseInt(inputThreads.value),
        show_hints: inputShowHints.checked,
        move_analysis: inputMoveAnalysis.checked,
        depth_bar: inputDepthBar.checked,
        evaluation_bar: inputEvalBar.checked,
        use_nnue: inputUseNNUE.checked,
        auto_move: inputAutoMove.checked,
    };
    chrome.storage.sync.set(options);
    chrome.tabs.query({}, function (tabs) {
        tabs.forEach(function (tab) {
            chrome.tabs.sendMessage(tab.id, { type: "UpdateOptions", data: options });
        });
    });
    // chrome.tabs.query({ active: true, currentWindow: true }, function (tabs)
    // {
    //     if (tabs[0].id === undefined) return;
    //     chrome.tabs.sendMessage(tabs[0].id, { type: "getText" }, function (response)
    //     {
    //         alert(response)
    //     });
    // });
}
function InitOptions() {
    inputDepth = document.getElementById("option-depth");
    inputThreads = document.getElementById("option-threads");
    inputShowHints = document.getElementById("option-show-hints");
    inputMoveAnalysis = document.getElementById("option-move-analysis");
    inputDepthBar = document.getElementById("option-depth-bar");
    inputEvalBar = document.getElementById("option-evaluation-bar");
    inputUseNNUE = document.getElementById("option-use-nnue");
    inputAutoMove = document.getElementById("option-auto-move");
    const sliderProps = {
        fill: "#2CA137",
        background: "rgba(255, 255, 255, 0.214)",
    };
    document.querySelectorAll(".options-slider").forEach(function (slider) {
        const title = slider.querySelector(".title");
        const input = slider.querySelector("input");
        if (title == null || input == null)
            return;
        input.min = slider.getAttribute("data-min");
        input.max = slider.getAttribute("data-max");
        input.addEventListener("input", (event) => {
            const value = parseInt(input.value);
            const minValue = parseInt(input.min);
            const maxValue = parseInt(input.max);
            const percent = (value - minValue) / (maxValue - minValue) * 100;
            const bg = `linear-gradient(90deg, ${sliderProps.fill} ${percent}%, ${sliderProps.background} ${percent + 0.1}%)`;
            input.style.background = bg;
            title.setAttribute("data-value", input.value);
            if (!event.disableUpdate)
                OnOptionsChange();
        });
    });
    document.querySelectorAll(".options-checkbox").forEach(function (checkbox) {
        checkbox.addEventListener("change", function () {
            OnOptionsChange();
        });
    });
    RestoreOptions();
}
document.addEventListener('DOMContentLoaded', InitOptions);
//# sourceMappingURL=options.js.map