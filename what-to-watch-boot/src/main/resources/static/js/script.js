var filmsApp = angular.module('filmsApp', ['infinite-scroll']);

filmsApp.controller('FilmListController', function ($scope, FilmList) {
  $scope.filmList = new FilmList();
});

filmsApp.factory('FilmList', function($http) {
	var FilmList = function() {
		this.items = [];
		this.busy = false;
		this.page = 1;
	};

	FilmList.prototype.nextPage = function() {
		if (this.busy) return;
		this.busy = true;
		console.log("Loading films for page: " + this.page);

		$http
			.get("/resources/suggestions/" + this.page)
			.success(function(data) {
				var items = data;
				for (var i = 0; i < items.length; i++) {
					this.items.push(items[i]);
				}
				this.page += 1;
				this.busy = false;
			}.bind(this));
	};

	return FilmList;
});