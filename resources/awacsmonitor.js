'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('xxmonitor', ['googlechart', 'ui.bootstrap']);


// appCommand.config();
$('#waitanswer').hide();

// Constant used to specify resource base path (facilitates integration into a Bonita custom page)
appCommand.constant('RESOURCE_PATH', 'pageResource?page=custompage_awacs&location=');






// --------------------------------------------------------------------------
//
// Controler MainControler
//
// --------------------------------------------------------------------------

appCommand.controller('MainController',
	function () {

	this.isshowhistory = false;

	this.showhistory = function( show )
	{
	   this.isshowhistory = show;
	}



});



// --------------------------------------------------------------------------
//
// Controler MonitorProcessController
//
// --------------------------------------------------------------------------

appCommand.controller('MonitorProcessController',
	function ($scope,$http) {
		$('#collectProcessesbtn').show();
		$('#collectProcessesWait').hide();
		this.alldetails=true;
		this.processes = [ ];
		this.isshowlegend=false;
		this.defaultWarningNbOverflowTasks=20;
		this.defaultWarningNearbyTasks=50;
		this.defaultWarningNbTasks=0;
		this.activityPeriodInMn=120;
		this.defaultmaxitems=1000;

		this.showlegend = function( show )
		{
			this.isshowlegend = show;
		};
		this.refresh = function()
		{

			$('#collectProcessesbtn').hide();
			$('#collectProcessesWait').show();

			var postMsg = {
					defaultWarningNearbyTasks: this.defaultWarningNearbyTasks,
					defaultWarningNbOverflowTasks: this.defaultWarningNbOverflowTasks,
					defaultWarningNbTasks: this.defaultWarningNbTasks,
					activityPeriodInMn: this.activityPeriodInMn,
					defaultmaxitems: this.defaultmaxitems,
					studypastactivities: this.studypastactivities,
				};



			var self = this;
			$http.get( '?page=custompage_awacs&action=monitoringprocess&paramjson='+ angular.toJson(postMsg, true))
			  .success(function success(jsonResult) {
				console.log('receive ',jsonResult);
				self.processes 						= jsonResult.processes;
				self.errormessage 					= jsonResult.errormessage;

				console.log(self.processes);
				$('#collectProcessesbtn').show();
				$('#collectProcessesWait').hide();
				})
			  .error( function error( result ) {
				self.errormessage = 'error during getInfo';
				$('#collectProcessesbtn').show();
				$('#collectProcessesWait').hide();
				}
			);

		};



	});





// --------------------------------------------------------------------------
//
// Controler MonitorUserController
//
// --------------------------------------------------------------------------

appCommand.controller('MonitorUserController',
	function ($scope,$http) {
		$('#collectUserbtn').show();
		$('#collectUserWait').hide();
		this.alldetails=true;
		this.users = [ ];
		this.isshowlegend=false;
		this.activityPeriodInMn=120;
		this.defaultmaxitems=1000;

		this.showlegend = function( show )
		{
			this.isshowlegend = show;
		};
		this.refresh = function()
		{

			$('#collectUserbtn').hide();
			$('#collectUserWait').show();

			var postMsg = {
					activityPeriodInMn: this.activityPeriodInMn,
					defaultmaxitems: this.defaultmaxitems,
					filtergroup : this.filtergroup,
					filterrole : this.filterrole,
					filteruser : this.filteruser,
				};



			var self = this;
			$http.get( '?page=custompage_awacs&action=monitoringuser&paramjson='+ angular.toJson(postMsg, true))
			  .success(function success(jsonResult) {
				console.log('receive ',jsonResult);
				self.users 					= jsonResult.users;
				self.errormessage 			= jsonResult.errormessage;
				self.graphtasksperusers    	= jsonResult.graphtasksperusers;
				$('#collectUserbtn').show();
				$('#collectUserWait').hide();
				})
			  .error( function error( result ) {
				self.errormessage = 'error during getInfo';
				$('#collectUserbtn').show();
				$('#collectUserWait').hide();
				}
			);

		};



	});


})();