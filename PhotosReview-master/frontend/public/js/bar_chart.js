

function drawBar(titles, values, chart, setting = [0,0,0], targetId) {
    console.log(setting);
    chart.innerHTML = "";
    const maxValue = Math.max(...values);
    const avgValue = values.reduce((a, b) => a + b, 0) / values.length;
    // 绘制柱子
    values.forEach((v, i) => {
        const container = document.createElement("div");
        container.classList.add("bar-container");
        if (targetId === 'disputeReasonHistogram') {
            container.classList.add("gap");
        }

        const bar = document.createElement("div");
        bar.className = "bar";
        // 计算柱子像素高度
        const barHeight = v / maxValue * 250; // 250px 高度比例
        setTimeout(() => { bar.style.height = barHeight + "px"; }, 100);

        // 新增：数值标签
        const valueLabel = document.createElement("div");
        valueLabel.className = "value-label";
        // 是否为直方图
        if(setting[0]){
            setTimeout(() => { valueLabel.style = "bottom: " + (barHeight + 25) + "px"; }, 100);
            // valueLabel.style = "bottom: " + (barHeight + 25) + "px"
        }
        valueLabel.innerText = v;
        container.appendChild(valueLabel);

        const label = document.createElement("div");
        label.className = "bar-label";
        label.innerText = titles[i];

        container.appendChild(bar);
        container.appendChild(label);
        chart.appendChild(container);
    });

    // 平均值线
    const avgLine = document.createElement("div");
    avgLine.className = "avg-line";
    avgLine.style.bottom = (avgValue / maxValue * 250 + 40) + "px";
    avgLine.style.width = chart.scrollWidth*2 + "px";

    const avgLabel = document.createElement("div");
    avgLabel.className = "avg-label";
    avgLabel.innerText = `平均值：${avgValue.toFixed(2)}`;
    avgLine.appendChild(avgLabel);

    chart.appendChild(avgLine);

    // 判断
    if(titles.length <= 8) {
        chart.style = "justify-content: center";
    }
    else{
        chart.style = "justify-content: left"
    }
}