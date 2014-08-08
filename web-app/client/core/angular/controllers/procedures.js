'use strict';

define(['helpers'], function (helpers) {

  /* Items */

  var Ctrl = ['$scope', '$interval', 'dataFactory', 'POLLING_INTERVAL',
    function($scope, $interval, dataFactory, POLLING_INTERVAL) {

    var statusEndpoints = [];
    $scope.procedures = [];

    var intervals = [];

    dataFactory.getProcedures(function (procedures) {
      $scope.procedures = procedures;
    });

    $scope.$on("$destroy", function() {

      if (typeof intervals !== 'undefined') {
        helpers.cancelAllIntervals($interval, intervals);
      }
    });

  }];

  return Ctrl;

});