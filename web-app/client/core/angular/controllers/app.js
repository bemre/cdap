'use strict';

define(['helpers'], function (helpers) {

  /* Items */

  var Ctrl = ['$scope', '$interval', '$routeParams', 'dataFactory',
    'POLLING_INTERVAL',
    function ($scope, $interval, $routeParams, dataFactory,
      POLLING_INTERVAL) {

    /**
     * @type {App}
     */
    $scope.app = {};

    var intervals = [];
    var statusEndpoints = [];

    dataFactory.getApp($routeParams.appId, function (app) {
      $scope.app = app;

      dataFactory.getStreamsByApp($scope.app.id, function (streams) {
        $scope.app.streams = streams;
      });

      dataFactory.getFlowsByApp($scope.app.id, function (flows) {
        $scope.app.flows = flows;
      });

      dataFactory.getMapreducesByApp($scope.app.id, function (mapreduces) {
        $scope.app.mapreduces = mapreduces;
      });

      dataFactory.getWorkflowsByApp($scope.app.id, function (workflows) {
        $scope.app.workflows = workflows;
      });

      dataFactory.getDatasetsByApp($scope.app.id, function (datasets) {
        $scope.app.datasets = datasets;
      });

      dataFactory.getProceduresByApp($scope.app.id, function (procedures) {
        $scope.app.procedures = procedures;
      });

    });

    $scope.getStatusEndpoint = function (entity) {
      return helpers.getStatusEndpoint(entity);
    };


    /**
     * Gets triggered on every route change, cancel all activated intervals.
     */
    $scope.$on("$destroy", function () {

      if (typeof intervals !== 'undefined') {
        helpers.cancelAllIntervals($interval, intervals);
      }
    });
  }];

  return Ctrl;

});