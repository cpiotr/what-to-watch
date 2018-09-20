var filmsApp = angular.module('filmsApp', ['infinite-scroll']);

filmsApp.controller('FilmListController', function ($scope, FilmList) {
  $scope.filmList = new FilmList();
});

filmsApp.factory('FilmList', function($http) {
    var FilmList = function() {
        this.displayedTitles = new Set();
        this.items = [];
        this.busy = false;
        this.page = 1;
    };

    FilmList.prototype.nextPage = function() {
        if (this.busy) return;

        var self = this;
        this.busy = true;
        console.log("Loading films for page: " + this.page);

        $http
            .get("/resources/suggestions/" + this.page)
            .success(function(data) {
                var receivedFilms = data;
                for (var i = 0; i < receivedFilms.length; i++) {
                    var titleWithYear = receivedFilms[i].formatTitle();
                    if (!this.displayedTitles.has(titleWithYear)) {
                        this.displayedTitles.add(titleWithYear)
                        this.items.push(receivedFilms[i]);
                    }
                }
                this.page += 1;
                this.busy = false;
                if (receivedFilms.length == 0 || this.items.length <= 3) {
                    self.nextPage();
                }
            }.bind(this));
    };

    return FilmList;
});

Object.prototype.formatTitle = function() {
    return this.title + "║" + this.year
}
