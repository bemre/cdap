'use strict';

define(['helpers'], function (helpers) {

  /* Items */

  var Ctrl = ['$scope', 'metricsService', '$interval', 'POLLING_INTERVAL',
    function($scope, metricsService, $interval, POLLING_INTERVAL) {

    /**
     * A list of intervals.
     */
    var intervals = [];

    /**
     * List of metrics to track.
     */
    var metrics = [
      { name: 'collect', endpoint: '/reactor/collect.events?count=60&start=now-65s&end=now-5s' },
      { name: 'process', endpoint: '/reactor/process.busyness?count=60&start=now-65s&end=now-5s'},
      { name: 'store', endpoint: '/reactor/dataset.store.bytes?count=60&start=now-65s&end=now-5s'},
      { name: 'query', endpoint: '/reactor/query.requests?count=60&start=now-65s&end=now-5s'}
    ];

    /**
     * Collect metric shown on Overview dashboard.
     * @type {Number}
     */
    $scope.collect = [];

    /**
     * Process metric shown on Overview dashboard.
     * @type {Number}
     */
    $scope.process = [];

    /**
     * Store metric shown on Overview dashboard.
     * @type {Number}
     */
    $scope.store = [];

    /**
     * Query metric shown on Overview dashboard.
     * @type {Number}
     */
    $scope.query = [];


    // Set up tracking for all metrics that need to be updated realtime.
    metrics.forEach(function (metric) {
      metricsService.trackMetric(metric.endpoint);
    });

    var ival = $interval(function () {
      metrics.forEach(function (metric) {
        $scope[metric.name] = metricsService.getMetricByEndpoint(metric.endpoint);
      });
    }, POLLING_INTERVAL);
    intervals.push(ival);

    //Custom to this controller and template.
    $scope.getMetricDisplayValue = function (val) {
      if (Object.prototype.toString.call(val) === '[object Array]' && val.length) {
        return helpers.getLastValue(val);
      }
    };

    /**
     * Gets triggered on every route change, cancel all activated intervals.
     */
    $scope.$on("$destroy", function() {
      for (var i = 0, len = metrics.length; i < len; i++) {
        metricsService.untrackMetric(metrics[i].endpoint);
      }
      if (typeof intervals !== 'undefined') {
        helpers.cancelAllIntervals($interval, intervals);
      }
    });
  }];

  return Ctrl;

});