bridge.controller('BloodPressureController', ['$scope', 'healthDataService', 'dashboardService', '$humane', 
function($scope, healthDataService, dashboardService, $humane) {
    
    if ($scope.recordToEdit) {
        $scope.systolic = $scope.recordToEdit.data.systolic;
        $scope.diastolic = $scope.recordToEdit.data.diastolic;
        $scope.date = new Date($scope.recordToEdit.startDate);
    } else {
        // This is changed to midnight in createPayload() below
        $scope.date = new Date();
    }

    // Ugly workaround for the fact that the form isn't available on the scope.
    // This is the simplest workaround I have found.
    $scope.setFormReference = function(bpForm) { $scope.bpForm = bpForm; };
    
    // somehow, this gets set as the default in the calendar control, go figure
     
    $scope.opened = false;
    $scope.format = 'MM/dd/yyyy';

    $scope.today = function() {
        $scope.bpForm.date.$setModelValue(new Date());
    };
    $scope.clear = function () {
        $scope.bpForm.date.$setModelValue(null);
    };
    // Disable after today
    $scope.disabled = function(date, mode) {
        return date.getTime() > new Date().getTime();
    };
    $scope.open = function(event) {
        $scope.opened = true; // contrary to docs, this does nothing
        setTimeout(function() { // however, this does work
            document.getElementById('datePicker').focus();    
        }, 1);
    };
    $scope.canSave = function() {
        // Have to test for presence of form because it's not immediately available,
        // because of transclusion weirdness.
        return ($scope.bpForm && $scope.bpForm.$dirty && $scope.bpForm.$valid);
    };
    $scope.canUpdate = function() {
        // Have to test for presence of form because it's not immediately available,
        // because of transclusion weirdness.
        return ($scope.bpForm && $scope.bpForm.$valid);
    };
    $scope.save = function() {
        var payload = healthDataService.createPayload($scope.bpForm, ['date', 'date'], ['systolic', 'diastolic'], true);
        var chartScope = $scope.$parent;
        healthDataService.create(chartScope.tracker.id, payload).then(function(data) {
            payload.recordId = data.payload.ids[0];
            chartScope.dataset.convertOne(payload);
        }, function(data) {
            $humane.error(data.payload);
        });
        $scope.cancel();
    };
    $scope.update = function() {
        var payload = healthDataService.updateRecord($scope.recordToEdit, 
                $scope.bpForm, ['date', 'date'], ['systolic', 'diastolic']);
        var chartScope = $scope.$parent;
        chartScope.dataset.update(payload);
        healthDataService.update(chartScope.tracker.id, payload).then(function(data) {
        }, function(data) {
            $humane.error(data.payload);
        });
        $scope.cancel();
    };
    $scope.cancel = function () {
        $scope.modalInstance.dismiss('cancel');
    };
    
}]);