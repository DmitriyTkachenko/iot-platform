$(document).ready(function () {
  var deviceId = "test_client";

  Highcharts.setOptions({
    global: {
      useUTC: false
    }
  });

  var chart = new Highcharts.StockChart({
    timeSeries: [],

    chart: {
      renderTo: 'container'
    },

    rangeSelector: {
      buttons: [{
        count: 1,
        type: 'minute',
        text: '1M'
      }, {
        count: 5,
        type: 'minute',
        text: '5M'
      }, {
        type: 'all',
        text: 'All'
      }],
      inputEnabled: false,
      selected: 0
    },

    title: {
      text: 'Data'
    },

    exporting: {
      enabled: true
    },

    series: [{
      name: 'Data',
      data: []
    }]
  }, function () {
    loadData();
  });

  function loadData() {
    $.ajax('http://localhost:8080/api/report/device/' + deviceId, {
      success: function (data) {
        data = JSON.parse(data);
        var series = chart.series[0];
        var processedData = data.map(function (item) {
          return [Date.parse(item.timestamp), item.temperatureStats.avg];
        });
        series.setData(processedData);
        console.log('Successfully loaded data');
        setTimeout(loadData, 1000);
      },
      error: function (error) {
        console.log('Error occurred: ' + error);
      }
    });
  }
});