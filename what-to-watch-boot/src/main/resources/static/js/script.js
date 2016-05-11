var filmsApp = angular.module('filmsApp', []);

filmsApp.controller('FilmListController', function ($scope, $http) {
	$http
            .get('/resources/suggestions')
            .success(function(data) {
                $scope.films = data;
            });
});
