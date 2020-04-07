class Film extends React.Component {
  render() {
    let film = this.props.value;
    return (
        <div className="col-sm-4 mb-4 d-flex">
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
                    <ScorePanel key={'score-panel'} totalScore={film.score} scores={film.scores} />
                    <GenrePanel key={'genre-panel'} genres={film.genres} />
                </div>
            </div>
        </div>
      );
  }
}

class ScorePanel extends React.Component {
  render() {
    let scores = this.props.scores.map((score, index) => {
        return (
            <Score key={'score' + index} value={score}/>
        )
    });
    return (
        <p>
            <div>
                <div className="btn btn-primary" role="button">
                    <span className="oi oi-star" aria-hidden="true"></span>
                    <span className="badge badge-light">
                        {(this.props.totalScore * 100).toFixed(0)}%
                    </span>
                </div>
            </div>
            <ul className="list-group">
                {scores}
            </ul>
        </p>
      );
  }
}

class Score extends React.Component {
  render() {
    let score = this.props.value;
    return (
        <li className="list-group-item">
            <span className="border badge badge-light oi oi-person float-right mr-1">{score.quantity}</span>
            <span className="border badge badge-light oi oi-star float-right mr-1">{(score.grade * 100).toFixed(0)}%</span>
            <a href={score.url}>{score.source}</a>
        </li>
      );
  }
}

class GenrePanel extends React.Component {
  render() {
    let genres = this.props.genres.map((genre, index) => {
        return (
            <Genre key={'genre' + index} name={genre}/>
        )
    });
    return (
        <p className="genres">
            {genres}
        </p>
      );
  }
}


class Genre extends React.Component {
  render() {
    let name = this.props.name;
    return (
        <span className="badge badge-pill badge-info" style={{marginRight: '2px'}}>
            {name}
        </span>
      );
  }
}

function Loader(props) {
    return (
        <div class="mx-auto loader"></div>
    );
}

function NextPage(props) {
    return (
        <button onClick={props.onClick}>Next</button>
    );
}

class FilmApp extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      films: [],
      filmIds: {},
      index: 1,
      loading: false
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
        this.setState({
            loading: false
        });
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
      index: currentIndex + 1,
      loading: true
    });
  }

  render() {
    if (this.state.index == 1) {
        this.loadPage();
    }

    const current = this.state;
    let films = current.films.map((film, index) => {
        return (
            <Film key={film.id} value={film} />
        )
    });

    let button;
    if (current.loading) {
        button = <Loader />
    } else {
        button = <NextPage onClick={() => this.loadPage()} />
    }

    return (
      <div className="filmsmain">
        <div className="films row">
          {films}
        </div>
        <div className="footer">
          {button}
        </div>
      </div>
    );
  }
}

ReactDOM.render(<FilmApp />, document.getElementById("root"));
