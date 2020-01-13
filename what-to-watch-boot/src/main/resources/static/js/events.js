class Film extends React.Component {
  render() {
    let film = this.props.value;
    let scores = film.scores.map((score, index) => {
        return (
            <Score value={score}/>
        )
    });
    return (
        <div class="col-sm-4 mb-4 d-flex ng-scope">
            <div className="col-sm p-3 border rounded shadow-sm filmbox">
                <div className="text-center mb-2">
                    <a className="btn btn-outline-primary p-1" href={film.link}>
                        <img className="poster" src={film.poster} alt={film.title}/>
                    </a>
                </div>
                <div className="caption">
                    <h3>
                        <a href={film.link}>{film.title} ({film.year})</a>
                    </h3>
                    <p>{film.plot}</p>
                    <p>
                        <div>
                            <div className="btn btn-primary" role="button">
                                <span className="oi oi-star" aria-hidden="true"></span>
                                <span className="badge badge-light">
                                    {(film.score * 100).toLocaleString(navigator.language, { minimumFractionDigits: 0 })}%
                                </span>
                            </div>
                        </div>
                        <ul class="list-group">
                            {scores}
                        </ul>
                    </p>
                </div>
            </div>
        </div>
      );
  }
}

class Score extends React.Component {
  render() {
    let score = this.props.value;
    return (
        <li class="list-group-item" ng-repeat="score in film.scores">
            <span class="border badge badge-light oi oi-person float-right mr-1">{score.quantity}</span>
            <span class="border badge badge-light oi oi-star float-right mr-1">{score.grade * 100}%</span>
            <a href={score.url}>{score.source}</a>
        </li>
      );
  }
}

class FilmApp extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      films: [],
      filmIds: {},
      index: 1
    };
  }

  loadPage() {
    const currentIndex = this.state.index;
    const current = this.state;
    const eventSource = new EventSource(`http://localhost:8080/resources/suggestions/${currentIndex}/stream`);
    eventSource.onError = function(event) {
        eventSource.close();
    }
    eventSource.addEventListener('poisonPill', (e) => {
        eventSource.close();
    });
    eventSource.addEventListener('film', (e) => {
        let film = JSON.parse(e.data)
        if (!current.filmIds.hasOwnProperty(e.lastEventId)) {
            current.films.push(film)
            current.filmIds[e.lastEventId] = true
            this.setState({
                films: current.films,
                filmIds: current.filmIds
            });
        }
    });

    this.setState({
      index: currentIndex + 1
    });
  }

  render() {
    const current = this.state;
    let films = current.films.map((film, index) => {
        return (
            <Film
                value={film}
                onClick={i => alert(i)}
            />
        )
    });

    return (
      <div className="filmsmain">
        <div className="films row">
          {films}
        </div>
        <div className="footer">
          <div>{status}</div>
          <button onClick={() => this.loadPage()}>Next</button>
        </div>
      </div>
    );
  }
}

ReactDOM.render(<FilmApp />, document.getElementById("root"));
