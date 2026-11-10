// dashboard-default.js
'use strict'
import ApexCharts from 'apexcharts'

export function initDashboardCharts() {
  console.log('aaa');
  
  setTimeout(() => {
    floatchart()
  }, 500)
}

function floatchart() {
  // 訪客圖表
  renderAreaChart('#visitor-chart', {
    name1: 'Page Views',
    data1: [31, 40, 28, 51, 42, 109, 100],
    name2: 'Sessions',
    data2: [11, 32, 45, 32, 34, 52, 41],
    categories: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  })

  // 月報圖表
  renderAreaChart('#visitor-chart-1', {
    name1: 'Page Views',
    data1: [76, 85, 101, 98, 87, 105, 91, 114, 94, 86, 115, 35],
    name2: 'Sessions',
    data2: [110, 60, 150, 35, 60, 36, 26, 45, 65, 52, 53, 41],
    categories: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  })

  // 收入圖表
  renderBarChart('#income-overview-chart')

  // 分析報表
  renderLineChart('#analytics-report-chart')

  // 銷售報表
  renderGroupedBarChart('#sales-report-chart')
}

function renderAreaChart(selector, { name1, data1, name2, data2, categories }) {
  const options = {
    chart: {
      height: 450,
      type: 'area',
      toolbar: { show: false }
    },
    dataLabels: { enabled: false },
    colors: ['#1890ff', '#13c2c2'],
    series: [
      { name: name1, data: data1 },
      { name: name2, data: data2 }
    ],
    stroke: {
      curve: 'smooth',
      width: 2
    },
    xaxis: { categories }
  }
  const chart = new ApexCharts(document.querySelector(selector), options)
  chart.render()
}

function renderBarChart(selector) {
  const options = {
    chart: {
      type: 'bar',
      height: 365,
      toolbar: { show: false }
    },
    colors: ['#13c2c2'],
    plotOptions: {
      bar: {
        columnWidth: '45%',
        borderRadius: 4
      }
    },
    dataLabels: { enabled: false },
    series: [{ data: [80, 95, 70, 42, 65, 55, 78] }],
    stroke: {
      curve: 'smooth',
      width: 2
    },
    xaxis: {
      categories: ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'],
      axisBorder: { show: false },
      axisTicks: { show: false }
    },
    yaxis: { show: false },
    grid: { show: false }
  }
  const chart = new ApexCharts(document.querySelector(selector), options)
  chart.render()
}

function renderLineChart(selector) {
  const options = {
    chart: {
      type: 'line',
      height: 340,
      toolbar: { show: false }
    },
    colors: ['#faad14'],
    stroke: {
      curve: 'smooth',
      width: 1.5
    },
    grid: {
      strokeDashArray: 4
    },
    series: [{
      data: [58, 90, 38, 83, 63, 75, 35, 55]
    }],
    xaxis: {
      type: 'datetime',
      categories: [
        '2018-05-19T00:00:00.000Z',
        '2018-06-19T00:00:00.000Z',
        '2018-07-19T01:30:00.000Z',
        '2018-08-19T02:30:00.000Z',
        '2018-09-19T03:30:00.000Z',
        '2018-10-19T04:30:00.000Z',
        '2018-11-19T05:30:00.000Z',
        '2018-12-19T06:30:00.000Z'
      ],
      labels: { format: 'MMM' },
      axisBorder: { show: false },
      axisTicks: { show: false }
    },
    yaxis: { show: false }
  }
  const chart = new ApexCharts(document.querySelector(selector), options)
  chart.render()
}

function renderGroupedBarChart(selector) {
  const options = {
    chart: {
      type: 'bar',
      height: 430,
      toolbar: { show: false }
    },
    plotOptions: {
      bar: {
        columnWidth: '30%',
        borderRadius: 4
      }
    },
    stroke: {
      show: true,
      width: 8,
      colors: ['transparent']
    },
    dataLabels: { enabled: false },
    legend: {
      position: 'top',
      horizontalAlign: 'right',
      show: true,
      fontFamily: `'Public Sans', sans-serif`,
      offsetX: 10,
      offsetY: 10,
      labels: { useSeriesColors: false },
      markers: {
        width: 10,
        height: 10,
        radius: '50%',
        offsexX: 2,
        offsexY: 2
      },
      itemMargin: {
        horizontal: 15,
        vertical: 5
      }
    },
    colors: ['#faad14', '#1890ff'],
    series: [
      { name: 'Net Profit', data: [180, 90, 135, 114, 120, 145] },
      { name: 'Revenue', data: [120, 45, 78, 150, 168, 99] }
    ],
    xaxis: {
      categories: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun']
    }
  }
  const chart = new ApexCharts(document.querySelector(selector), options)
  chart.render()
}
