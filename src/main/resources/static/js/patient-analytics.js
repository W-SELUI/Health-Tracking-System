"use strict";

document.addEventListener("DOMContentLoaded", () => {

    renderScalarMetricCharts();
    renderBloodPressureChart();

    renderDailyBarChart(
            "daily-steps-chart",
            "steps"
    );

    renderDailyBarChart(
            "daily-duration-chart",
            "minutes"
    );

    renderDailyBarChart(
            "daily-calories-chart",
            "kcal"
    );

    renderExerciseTypeChart();
});


/*
 * ============================================================
 * COMMON SVG HELPERS
 * ============================================================
 */

const SVG_NAMESPACE = "http://www.w3.org/2000/svg";

const CHART_WIDTH = 800;
const CHART_HEIGHT = 320;

const MARGIN = {
    top: 25,
    right: 30,
    bottom: 60,
    left: 75
};


function createSvgElement(name, attributes = {}) {

    const element =
            document.createElementNS(
                    SVG_NAMESPACE,
                    name
            );

    Object.entries(attributes)
            .forEach(([key, value]) => {
                element.setAttribute(
                        key,
                        value
                );
            });

    return element;
}


function createChartSvg() {

    return createSvgElement(
            "svg",
            {
                viewBox:
                        `0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`,
                preserveAspectRatio:
                        "xMidYMid meet",
                class: "analytics-chart-svg",
                role: "img"
            }
    );
}


function appendText(
        svg,
        x,
        y,
        text,
        className,
        anchor = "middle") {

    const label =
            createSvgElement(
                    "text",
                    {
                        x: x,
                        y: y,
                        class: className,
                        "text-anchor": anchor
                    }
            );

    label.textContent = text;

    svg.appendChild(label);
}


function formatDate(value) {

    const date =
            new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleDateString(
            undefined,
            {
                day: "numeric",
                month: "short"
            }
    );
}


function calculateRange(values) {

    let minimum =
            Math.min(...values);

    let maximum =
            Math.max(...values);

    /*
     * A graph with all identical values would otherwise have
     * a zero-height y-axis.
     */
    if (minimum === maximum) {

        const padding =
                minimum === 0
                        ? 1
                        : Math.abs(minimum) * 0.1;

        minimum -= padding;
        maximum += padding;
    }

    const range =
            maximum - minimum;

    const extraPadding =
            range * 0.1;

    return {
        minimum:
                minimum - extraPadding,

        maximum:
                maximum + extraPadding
    };
}


function yCoordinate(
        value,
        minimum,
        maximum) {

    const plotHeight =
            CHART_HEIGHT
            - MARGIN.top
            - MARGIN.bottom;

    const ratio =
            (value - minimum)
            / (maximum - minimum);

    return CHART_HEIGHT
            - MARGIN.bottom
            - ratio * plotHeight;
}


function drawAxes(
        svg,
        minimum,
        maximum,
        unit) {

    const plotBottom =
            CHART_HEIGHT
            - MARGIN.bottom;

    const plotRight =
            CHART_WIDTH
            - MARGIN.right;

    /*
     * Y axis.
     */
    svg.appendChild(
            createSvgElement(
                    "line",
                    {
                        x1: MARGIN.left,
                        y1: MARGIN.top,
                        x2: MARGIN.left,
                        y2: plotBottom,
                        class: "chart-axis"
                    }
            )
    );

    /*
     * X axis.
     */
    svg.appendChild(
            createSvgElement(
                    "line",
                    {
                        x1: MARGIN.left,
                        y1: plotBottom,
                        x2: plotRight,
                        y2: plotBottom,
                        class: "chart-axis"
                    }
            )
    );

    const tickCount = 5;

    for (let i = 0; i <= tickCount; i++) {

        const ratio =
                i / tickCount;

        const value =
                minimum
                + (maximum - minimum)
                * ratio;

        const y =
                yCoordinate(
                        value,
                        minimum,
                        maximum
                );

        svg.appendChild(
                createSvgElement(
                        "line",
                        {
                            x1: MARGIN.left,
                            y1: y,
                            x2: plotRight,
                            y2: y,
                            class: "chart-grid-line"
                        }
                )
        );

        appendText(
                svg,
                MARGIN.left - 10,
                y + 4,
                formatChartNumber(value),
                "chart-axis-label",
                "end"
        );
    }

    if (unit) {

        appendText(
                svg,
                MARGIN.left,
                15,
                unit,
                "chart-unit-label",
                "start"
        );
    }
}


function formatChartNumber(value) {

    if (Math.abs(value) >= 1000) {

        return Math.round(value)
                .toLocaleString();
    }

    if (Number.isInteger(value)) {
        return value.toString();
    }

    return value.toFixed(1);
}


/*
 * ============================================================
 * SCALAR HEALTH METRIC LINE CHARTS
 * ============================================================
 */

function renderScalarMetricCharts() {

    const containers =
            document.querySelectorAll(
                    ".metric-line-chart"
            );

    containers.forEach(container => {

        const unit =
                container.dataset.unit || "";

        const rawPoints =
                Array.from(
                        container.querySelectorAll(
                                ".chart-data-point"
                        )
                );

        const points =
                rawPoints
                        .map(element => ({
                            time:
                                    element.dataset.time,

                            timestamp:
                                    new Date(
                                            element.dataset.time
                                    ).getTime(),

                            value:
                                    Number(
                                            element.dataset.value
                                    )
                        }))
                        .filter(point =>
                            Number.isFinite(point.value)
                            && Number.isFinite(point.timestamp)
                        )
                        .sort(
                                (a, b) =>
                                    a.timestamp - b.timestamp
                        );

        renderLineChart(
                container,
                points,
                unit
        );
    });
}


function renderLineChart(
        container,
        points,
        unit) {

    container.innerHTML = "";

    if (points.length === 0) {
        showChartEmptyMessage(container);
        return;
    }

    const svg =
            createChartSvg();

    const values =
            points.map(
                    point => point.value
            );

    const range =
            calculateRange(values);

    drawAxes(
            svg,
            range.minimum,
            range.maximum,
            unit
    );

    const plotWidth =
            CHART_WIDTH
            - MARGIN.left
            - MARGIN.right;

    const firstTimestamp =
            points[0].timestamp;

    const lastTimestamp =
            points[points.length - 1].timestamp;

    const timestampRange =
            lastTimestamp - firstTimestamp;


    const coordinates =
            points.map((point, index) => {

                let x;

                if (points.length === 1) {

                    x =
                            MARGIN.left
                            + plotWidth / 2;

                } else if (timestampRange === 0) {

                    x =
                            MARGIN.left
                            + (
                                index
                                / (points.length - 1)
                            )
                            * plotWidth;

                } else {

                    x =
                            MARGIN.left
                            + (
                                (
                                    point.timestamp
                                    - firstTimestamp
                                )
                                / timestampRange
                            )
                            * plotWidth;
                }

                const y =
                        yCoordinate(
                                point.value,
                                range.minimum,
                                range.maximum
                        );

                return {
                    ...point,
                    x,
                    y
                };
            });


    if (coordinates.length >= 2) {

        const polylinePoints =
                coordinates
                        .map(point =>
                            `${point.x},${point.y}`
                        )
                        .join(" ");

        svg.appendChild(
                createSvgElement(
                        "polyline",
                        {
                            points:
                                    polylinePoints,

                            class:
                                    "chart-line"
                        }
                )
        );
    }


    coordinates.forEach(point => {

        const circle =
                createSvgElement(
                        "circle",
                        {
                            cx: point.x,
                            cy: point.y,
                            r: 5,
                            class:
                                    "chart-point"
                        }
                );

        const title =
                createSvgElement(
                        "title"
                );

        title.textContent =
                `${formatDate(point.time)}: `
                + `${point.value} ${unit}`;

        circle.appendChild(title);

        svg.appendChild(circle);
    });


    appendTimeLabels(
            svg,
            coordinates
    );

    container.appendChild(svg);
}


/*
 * ============================================================
 * BLOOD PRESSURE DUAL LINE CHART
 * ============================================================
 */

function renderBloodPressureChart() {

    const container =
            document.getElementById(
                    "blood-pressure-chart"
            );

    if (!container) {
        return;
    }

    const rawPoints =
            Array.from(
                    container.querySelectorAll(
                            ".chart-data-point"
                    )
            );

    const points =
            rawPoints
                    .map(element => ({
                        time:
                                element.dataset.time,

                        timestamp:
                                new Date(
                                        element.dataset.time
                                ).getTime(),

                        systolic:
                                Number(
                                        element.dataset.systolic
                                ),

                        diastolic:
                                Number(
                                        element.dataset.diastolic
                                )
                    }))
                    .filter(point =>
                        Number.isFinite(
                                point.timestamp
                        )
                        && Number.isFinite(
                                point.systolic
                        )
                        && Number.isFinite(
                                point.diastolic
                        )
                    )
                    .sort(
                            (a, b) =>
                                a.timestamp - b.timestamp
                    );

    container.innerHTML = "";

    if (points.length === 0) {
        showChartEmptyMessage(container);
        return;
    }

    const values =
            points.flatMap(point => [
                point.systolic,
                point.diastolic
            ]);

    const range =
            calculateRange(values);

    const svg =
            createChartSvg();

    drawAxes(
            svg,
            range.minimum,
            range.maximum,
            "mmHg"
    );

    const plotWidth =
            CHART_WIDTH
            - MARGIN.left
            - MARGIN.right;

    const firstTimestamp =
            points[0].timestamp;

    const lastTimestamp =
            points[points.length - 1].timestamp;

    const timestampRange =
            lastTimestamp - firstTimestamp;


    const coordinates =
            points.map((point, index) => {

                let x;

                if (points.length === 1) {

                    x =
                            MARGIN.left
                            + plotWidth / 2;

                } else if (timestampRange === 0) {

                    x =
                            MARGIN.left
                            + (
                                index
                                / (points.length - 1)
                            )
                            * plotWidth;

                } else {

                    x =
                            MARGIN.left
                            + (
                                (
                                    point.timestamp
                                    - firstTimestamp
                                )
                                / timestampRange
                            )
                            * plotWidth;
                }

                return {
                    ...point,

                    x,

                    systolicY:
                            yCoordinate(
                                    point.systolic,
                                    range.minimum,
                                    range.maximum
                            ),

                    diastolicY:
                            yCoordinate(
                                    point.diastolic,
                                    range.minimum,
                                    range.maximum
                            )
                };
            });


    drawBloodPressureSeries(
            svg,
            coordinates,
            "systolic"
    );

    drawBloodPressureSeries(
            svg,
            coordinates,
            "diastolic"
    );

    appendTimeLabels(
            svg,
            coordinates
    );

    drawBloodPressureLegend(svg);

    container.appendChild(svg);
}


function drawBloodPressureSeries(
        svg,
        coordinates,
        type) {

    const yProperty =
            type === "systolic"
                    ? "systolicY"
                    : "diastolicY";

    if (coordinates.length >= 2) {

        const linePoints =
                coordinates
                        .map(point =>
                            `${point.x},${point[yProperty]}`
                        )
                        .join(" ");

        svg.appendChild(
                createSvgElement(
                        "polyline",
                        {
                            points:
                                    linePoints,

                            class:
                                    `chart-line blood-pressure-${type}`
                        }
                )
        );
    }


    coordinates.forEach(point => {

        const circle =
                createSvgElement(
                        "circle",
                        {
                            cx:
                                    point.x,

                            cy:
                                    point[yProperty],

                            r:
                                    5,

                            class:
                                    `chart-point blood-pressure-${type}`
                        }
                );

        const title =
                createSvgElement("title");

        title.textContent =
                `${formatDate(point.time)}: `
                + `${point.systolic}/${point.diastolic} mmHg`;

        circle.appendChild(title);

        svg.appendChild(circle);
    });
}


function drawBloodPressureLegend(svg) {

    const legendY = 25;

    appendText(
            svg,
            CHART_WIDTH - 180,
            legendY,
            "Systolic",
            "chart-legend blood-pressure-systolic-label",
            "start"
    );

    appendText(
            svg,
            CHART_WIDTH - 95,
            legendY,
            "Diastolic",
            "chart-legend blood-pressure-diastolic-label",
            "start"
    );
}


/*
 * ============================================================
 * ACTIVITY DAILY BAR CHARTS
 * ============================================================
 */

function renderDailyBarChart(
        elementId,
        unit) {

    const container =
            document.getElementById(
                    elementId
            );

    if (!container) {
        return;
    }

    const rawPoints =
            Array.from(
                    container.querySelectorAll(
                            ".chart-data-point"
                    )
            );

    const points =
            rawPoints.map(element => ({
                label:
                        element.dataset.date,

                value:
                        Number(
                                element.dataset.value
                        ),

                logCount:
                        Number(
                                element.dataset.logCount
                        )
            }));

    renderBarChart(
            container,
            points,
            unit,
            true
    );
}


/*
 * ============================================================
 * EXERCISE TYPE BAR CHART
 * ============================================================
 */

function renderExerciseTypeChart() {

    const container =
            document.getElementById(
                    "exercise-type-chart"
            );

    if (!container) {
        return;
    }

    const rawPoints =
            Array.from(
                    container.querySelectorAll(
                            ".chart-data-point"
                    )
            );

    const points =
            rawPoints.map(element => ({
                label:
                        formatExerciseType(
                                element.dataset.label
                        ),

                value:
                        Number(
                                element.dataset.value
                        ),

                logCount:
                        1
            }));

    renderBarChart(
            container,
            points,
            "min",
            false
    );
}


function renderBarChart(
        container,
        points,
        unit,
        distinguishMissingDays) {

    container.innerHTML = "";

    const validPoints =
            points.filter(point =>
                Number.isFinite(point.value)
            );

    if (validPoints.length === 0) {
        showChartEmptyMessage(container);
        return;
    }

    const maximum =
            Math.max(
                    1,
                    ...validPoints.map(
                            point => point.value
                    )
            );

    const range = {
        minimum: 0,
        maximum:
                maximum
                + maximum * 0.1
    };

    const svg =
            createChartSvg();

    drawAxes(
            svg,
            range.minimum,
            range.maximum,
            unit
    );

    const plotWidth =
            CHART_WIDTH
            - MARGIN.left
            - MARGIN.right;

    const plotBottom =
            CHART_HEIGHT
            - MARGIN.bottom;

    const slotWidth =
            plotWidth
            / validPoints.length;

    const barWidth =
            Math.max(
                    4,
                    slotWidth * 0.6
            );


    validPoints.forEach(
            (point, index) => {

                const centerX =
                        MARGIN.left
                        + slotWidth * index
                        + slotWidth / 2;

                const y =
                        yCoordinate(
                                point.value,
                                range.minimum,
                                range.maximum
                        );

                const height =
                        Math.max(
                                0,
                                plotBottom - y
                        );

                const missing =
                        distinguishMissingDays
                        && point.logCount === 0;


                if (!missing) {

                    const bar =
                            createSvgElement(
                                    "rect",
                                    {
                                        x:
                                                centerX
                                                - barWidth / 2,

                                        y:
                                                y,

                                        width:
                                                barWidth,

                                        height:
                                                height,

                                        class:
                                                "chart-bar"
                                    }
                            );

                    const title =
                            createSvgElement(
                                    "title"
                            );

                    title.textContent =
                            `${formatBarLabel(point.label)}: `
                            + `${point.value} ${unit}`;

                    bar.appendChild(title);

                    svg.appendChild(bar);

                } else {

                    /*
                     * Missing data is represented separately from a
                     * recorded zero-value activity day.
                     */
                    const marker =
                            createSvgElement(
                                    "circle",
                                    {
                                        cx:
                                                centerX,

                                        cy:
                                                plotBottom - 5,

                                        r:
                                                4,

                                        class:
                                                "chart-missing-marker"
                                    }
                            );

                    const title =
                            createSvgElement(
                                    "title"
                            );

                    title.textContent =
                            `${formatBarLabel(point.label)}: `
                            + "No activity data recorded";

                    marker.appendChild(title);

                    svg.appendChild(marker);
                }


                if (shouldShowCategoryLabel(
                        index,
                        validPoints.length
                )) {

                    appendText(
                            svg,
                            centerX,
                            plotBottom + 22,
                            formatBarLabel(
                                    point.label
                            ),
                            missing
                                    ? "chart-axis-label chart-missing-label"
                                    : "chart-axis-label"
                    );
                }
            }
    );

    container.appendChild(svg);
}


/*
 * ============================================================
 * LABEL HELPERS
 * ============================================================
 */

function appendTimeLabels(
        svg,
        points) {

    if (points.length === 0) {
        return;
    }

    const indexes =
            labelIndexes(
                    points.length
            );

    indexes.forEach(index => {

        const point =
                points[index];

        appendText(
                svg,
                point.x,
                CHART_HEIGHT - MARGIN.bottom + 25,
                formatDate(point.time),
                "chart-axis-label"
        );
    });
}


function labelIndexes(length) {

    if (length <= 1) {
        return [0];
    }

    if (length === 2) {
        return [0, 1];
    }

    const middle =
            Math.floor(
                    (length - 1) / 2
            );

    return Array.from(
            new Set([
                0,
                middle,
                length - 1
            ])
    );
}


function shouldShowCategoryLabel(
        index,
        length) {

    /*
     * With few bars, show every label.
     */
    if (length <= 7) {
        return true;
    }

    /*
     * For larger periods, show approximately seven labels.
     */
    const interval =
            Math.ceil(
                    length / 7
            );

    return index === 0
            || index === length - 1
            || index % interval === 0;
}


function formatBarLabel(label) {

    if (
        typeof label === "string"
        && /^\d{4}-\d{2}-\d{2}$/.test(label)
    ) {

        return formatDate(
                `${label}T00:00:00`
        );
    }

    return label;
}


function formatExerciseType(value) {

    if (!value) {
        return "";
    }

    return value
            .toLowerCase()
            .split("_")
            .map(word =>
                word.charAt(0).toUpperCase()
                + word.slice(1)
            )
            .join(" ");
}


function showChartEmptyMessage(
        container) {

    const message =
            document.createElement("p");

    message.className =
            "chart-empty-message";

    message.textContent =
            "No chart data is available.";

    container.appendChild(message);
}