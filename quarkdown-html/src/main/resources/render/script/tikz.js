async function waitForAllTikz() {
    const total = document.querySelectorAll('script[type="text/tikz"]').length;

    if (total === 0) return Promise.resolve();

    await new Promise(resolve => {
        let finished = 0;

        function onFinished() {
            if (++finished >= total)
            {
                resolve();
            }
        }
        document.addEventListener('tikzjax-load-finished', onFinished);
    });
}

function initTikz() {
    document.addEventListener('tikzjax-load-finished', () => { console.log("debug: invoke tikz load finished"); });
    preRenderingExecutionQueue.push(waitForAllTikz);
}