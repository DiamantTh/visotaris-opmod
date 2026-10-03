import * as echarts from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// Nur die in der History verwendeten Bausteine registrieren. Dieses Modul wird
// erst geladen, wenn tatsächlich Verlaufsdaten als Diagramm dargestellt werden.
echarts.use([LineChart, BarChart, GridComponent, TooltipComponent, CanvasRenderer])

export { echarts }
