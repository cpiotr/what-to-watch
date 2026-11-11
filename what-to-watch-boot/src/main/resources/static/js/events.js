class Film extends React.Component {
  render() {
    let film = this.props.value;
    return (
        <div className="col-sm-6 col-lg-4 mb-4 d-flex">
            <article className="film-card">
                <a className="poster-link" href={film.link}>
                    {film.poster
                        ? <img className="poster" src={film.poster} alt={film.title}/>
                        : <span className="poster poster-missing" role="img" aria-label={film.title}>
                              <span className="oi oi-video" aria-hidden="true"></span>
                          </span>}
                </a>
                <h3 className="film-title">
                    <a href={film.link}>{film.title}</a> <span className="film-year">({film.year})</span>
                </h3>
                {film.plot && <p className="film-plot">{film.plot}</p>}
                <ScorePanel key={'score-panel'} totalScore={film.score} predictedRating={film.predictedRating} scores={film.scores} />
                <GenrePanel key={'genre-panel'} genres={film.genres} />
            </article>
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
        <div>
            <div className="ratings">
                <span className="rating rating-score" title="Combined score">
                    <span className="oi oi-star" aria-hidden="true"></span>
                    {(this.props.totalScore * 100).toFixed(0)}%
                </span>
                {this.props.predictedRating &&
                    <span className="rating rating-predicted" title="My predicted rating">
                        <span className="oi oi-heart" aria-hidden="true"></span>
                        {(this.props.predictedRating * 10).toFixed(0)}%
                    </span>
                }
            </div>
            <ul className="score-list">
                {scores}
            </ul>
        </div>
      );
  }
}

class Score extends React.Component {
  render() {
    let score = this.props.value;
    return (
        <li>
            <a className="score-source" href={score.url}>{score.source}</a>
            <span className="score-grade" title="Score">
                <span className="oi oi-star" aria-hidden="true"></span>{(score.grade * 100).toFixed(0)}%
            </span>
            <span className="score-votes" title="Votes">
                <span className="oi oi-person" aria-hidden="true"></span>{score.quantity.toLocaleString('pl-PL')}
            </span>
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
        <span className="genre">
            {name}
        </span>
      );
  }
}

function Loader(props) {
    return (
        <div className="loader"></div>
    );
}

function NextPage(props) {
    return (
        <button className="load-more" onClick={props.onClick}>Next</button>
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
    const eventSource = new EventSource(`/resources/suggestions/${currentIndex}/stream`);
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
