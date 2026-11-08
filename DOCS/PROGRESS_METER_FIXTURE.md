# Progress and meter fixture

The values below are intentionally awkward:

<progress value="11" max="10">11/10</progress>

<meter min="0" max="1" low="0.2" high="0.8" optimum="0.5" value="2">2</meter>

Browsers may clamp, ignore, or restyle values outside the declared range.
There is no script or network source here; the elements only exercise HTML
rendering and fallback text.
