/*global define */
// TODO: resize to half on 100%
// Eliminate jerky motion
// pause on data delay, dont continue transition
// Emulate what is on ember right now
// No switch on data delay, everything must stop and move smoothly (extrapolate points)

'use strict';

define(function() {

  var Directive = ['$interval', 'metricsService', 'POLLING_INTERVAL',
    function($interval, metricsService, POLLING_INTERVAL) {

    var AnimatedSparkline = Class.create({

      initialize: function (elm, data, width, height, interpolation, animate, updateDelay,
        transitionDelay, percent, shade, dash) {
          var self = this;
          this.elm = elm;          

          var parent = $(self.elm).parent();
          this.width = 150;
          this.height = 30;
          this.data = data || [];
          this.interpolation = interpolation || 'basis';
          this.animate = animate || true;
          this.updateDelay = updateDelay || 3000;
          this.transitionDelay = transitionDelay || 3000;
          this.isPercent = percent || false;
          this.isShade = shade || false;
          this.formattedData = [];
          this.dataChanged = false;

          if (dash) {
            this.width = parent.outerWidth() || 300;
            this.height = parent.outerHeight() || 100;
          }

          this.updateData(data);

          var dataLength = this.formattedData.length;
          this.yBuffer = 5.0;
          var extend = Math.round(this.width / dataLength);
          var margin = 5;
          var max = d3.max(this.formattedData);
          var min = d3.min(this.formattedData);

          this.graph = d3.select(this.elm)
            .append('svg:svg')
            .attr('width', '100%')
            .attr('height', '100%')
            .attr('preserveAspectRatio', 'none');


          this.x = d3.scale.linear().domain([0, 55]).range([-5, this.width - 5]);


          this.y = d3.scale.linear().domain([max + this.yBuffer, min]).range([5, this.height - 5]);

          this.line = d3.svg.line()
            .x(function(d,i) {
              return self.x(i);
            })
            .y(function(d) {
              return self.y(d);
            }).interpolate(this.interpolation);

          this.g = this.graph.append('svg:g');

          if (this.isPercent || this.isShade) {
            this.area = d3.svg.area()
              .x(this.line.x())
              .y1(this.line.y())
              .y0(this.y(0));
            this.g.append('svg:path')
              .attr('class', 'sparkline-area')
              .attr('d', this.area(this.formattedData));  
          }

          this.g.append('svg:path')
            .attr('class', 'sparkline-data')
            .attr('d', this.line(this.formattedData));

          this.tDelay = transitionDelay;

          if (true) {
            self. interval = setInterval(function () {
              self.redrawWithAnimation();
            }, 3000);
          }

          // Set up resize handler.
          $(window).resize(self.resizerFn.bind(self));
      },

      resizerFn: function () {
        var self = this;
        var parent = $(self.elm).parent();
        this.width = parent.outerWidth();
        this.height = parent.outerHeight();
        $(self.elm).empty();

        // create an SVG element inside the #graph div that fills 100% of the div
        this.graph = d3.select(this.elm)
          .append('svg:svg')
          .attr('width', '100%')
          .attr('height', '100%')
          .attr('preserveAspectRatio', 'none');

        var max = d3.max(this.formattedData) || 100;
        var min = d3.min(this.formattedData) || 0;
        this.x = d3.scale.linear().domain([0, 55]).range([-5, this.width - 5]);


        this.y = d3.scale.linear().domain([max + this.yBuffer, min]).range([0, this.height - 5]);

        this.line = d3.svg.line()
          .x(function(d,i) {
            return self.x(i);
          })
          .y(function(d) {
            return self.y(d);
          }).interpolate(this.interpolation);

        // display the line by appending an svg:path element with the data line we created above
        this.g = this.graph.append('svg:g');
        this.g.append('svg:path').attr('class', 'sparkline-data').attr('d', this.line(this.formattedData));

      },

      redrawWithAnimation: function (force) {
        var self = this;
        if (this.dataChanged || force) {
          this.graph.selectAll('path.sparkline-data')
            .data([this.formattedData])
            .attr('transform', 'translate(' + this.x(0) + ')')
            .attr('d', this.line)
            .transition()
            .ease('linear')
            .duration(3000)
            .attr('transform', 'translate(' + this.x(-3) + ')');
        }
      },

      updateData: function (newData) {
        var self = this;
        this.data = newData;
        var formattedData = newData.map(function (entry) {
          return entry.value;
        });
        if (!angular.equals(formattedData, this.formattedData)) {
          this.formattedData = formattedData;
          this.dataChanged = true;
          var max = d3.max(this.formattedData) || 100;
          var min = d3.min(this.formattedData) || 0;
          this.x = d3.scale.linear().domain([0, 55]).range([-5, this.width - 5]);


          this.y = d3.scale.linear().domain([max + this.yBuffer, min]).range([0, this.height - 5]);

          this.line = d3.svg.line()
            .x(function(d,i) {
              return self.x(i);
            })
            .y(function(d) {
              return self.y(d);
            }).interpolate(this.interpolation);

        } else {
          this.dataChanged = false;
        }        
      },

      destroy: function () {
        $(window).off('resize', this.resizerFn);
        clearInterval(this.interval);
      }


    });

    return {

      restrict: 'AE',
      replace: true,
      scope: {
        area: '@',
        percent: '@',
        dash: '@',
        metricEndpoint: '@'
      },
      link: function (scope, elm, attrs) {
        if (!scope.metricEndpoint) {
          console.error('no metric specified for chart in div', elm);
          return;
        }

        metricsService.trackMetric(scope.metricEndpoint);

        var ival = $interval(function () {
          scope.data = metricsService.getMetricByEndpoint(scope.metricEndpoint);
        }, POLLING_INTERVAL);

        var isPercent = Boolean(scope.percent);
        var isShade = Boolean(scope.shade);
        var dash = Boolean(scope.dash);
        var ts = new Date().getTime();

        // Set default value to 0.
        var data = Array.apply(null, new Array(61)).map(
          Object.prototype.valueOf, {
            time: ++ts,
            value: 0
          }
        );
        var animatedSparkline = new AnimatedSparkline(
          elm[0], data, scope.width, scope.height, 'monotone', true, 3000, 3000, isPercent, isShade,
          dash);

        scope.$watch('data', function (newVal, oldVal) {
          if (newVal && angular.isArray(newVal.data) && newVal.data.length) {
            animatedSparkline.updateData(newVal.data);
          }
        });


        scope.$on('$destroy', function() {
          $interval.cancel(ival);
          metricsService.untrackMetric(scope.metricEndpoint);
          animatedSparkline.destroy();
        });
      }
    }

  }];

  return Directive;

});