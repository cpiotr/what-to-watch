document.domain = 'localhost:8080'

var filmsApp = angular.module('filmsApp', []);

filmsApp.controller('FilmListController', function ($scope, $http) {
	$http
            .get('http://localhost:8080/suggestions')
            .success(function(data) {
                $scope.films = data;
            });
});
