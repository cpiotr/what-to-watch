class Film extends React.Component {
  render() {
    return (
        <div className="col-sm p-3 border rounded shadow-sm filmbox">
            <div className="text-center mb-2">
                <a className="btn btn-outline-primary p-1" href="{this.props.value.link}">
                    <img className="poster" src="{this.props.value.poster}" alt="{this.props.value.title}"/>
                </a>
            </div>
            <div className="caption">
                <h3>
                    <a href="{this.props.value.link}">{this.props.value.title} ({this.props.value.year})</a>
                </h3>
                <p>{this.props.value.plot}</p>
                <div>
                    <div className="btn btn-primary" role="button">
                        <span className="oi oi-star" aria-hidden="true"></span>
                        <span className="badge badge-light">{this.props.value.score}%   </span>
                    </div>
                </div>
            </div>
        </div>
      );
  }
}

class FilmApp extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      films: [{title: 'Test', poster: 'Test123', score: 0.78, year: 1999}],
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
        current.films.push(JSON.parse(e.data));
        this.setState({
            films: current.films
        });
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
        <div className="films">
          {films}
        </div>
        <div className="game-info">
          <div>{status}</div>
          <button onClick={() => this.loadPage()}>Next</button>
        </div>
      </div>
    );
  }
}

ReactDOM.render(<FilmApp />, document.getElementById("root"));
