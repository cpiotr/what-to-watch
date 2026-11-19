# Could a better model recommend films?

Research from November 2026: whether any approach would predict my ratings better than `PersonalRatings`, the ridge
regression over Filmweb, IMDb and Metacritic scores. Measured on 1,000 of my ratings with repeated cross-validation,
and with older films used to predict newer ones, since the app mostly suggests new releases. Every cited claim was
checked against its source.

## The model stays
Nothing beat it on new releases, which is where the app matters most. It ranks my films well (Spearman 0.57; 84% of
its top 20 are films I rated 7 or more, against 42% of all my ratings), but only slightly better than IMDb or Filmweb
alone. Most of what I'd think of a film isn't in its aggregate scores, and no other model shape recovered it: gradient
boosting, ordinal regression, interactions and a tuned penalty all scored the same or worse.

## The threshold moves to 5.8
At 5.4, the model passed 84% of my rated titles and removed only half of those I rated 4 or less. That's where suggestions were
weakest, not in the model itself. Predictions from cross-validation, over all my rated films:

| Threshold | Rated 7+ kept | Rated 6+ kept | Rated 4 or less removed | Suggestions I'd rate 6+ |
|---|---|---|---|---|
| 5.4 | 99% | 92% | 52% | 81% |
| 5.7 | 94% | 84% | 62% | 83% |
| 5.8 | 92% | 79% | 70% | 85% |
| 6.0 | 85% | 69% | 78% | 88% |

Of the steps from 5.5 up, 5.7 to 5.8 removes the most bad films for each good one lost, though it rests on only 100
films rated 4 or less. Higher thresholds start dropping films I'd rate 7. This is a trade-off, not a proven optimum:
only one film from 2020 on that I rated is a 4 or less, most likely because I found recent films through the app
itself, so how well it filters bad new releases can't be measured yet.

## What didn't help, and why
- **Genres and runtime.** They predict ratings beyond aggregate scores across Netflix users
  ([Moon et al. 2010](https://doi.org/10.1509/jmkg.74.1.108)), but the effects are tiny, and three separate
  experiments here found no reliable gain and a loss on the newest films.
- **Vote counts and release year.** They look good in cross-validation, but the model learns that fewer votes mean a
  better film, because every film I rated has years of votes behind it. A new release would score 0.3 to 0.6 higher
  just for being new.
- **Other users' ratings from MovieLens.** Ranking improved by about 0.05 Spearman on older films, but MovieLens ends
  in October 2023, so it knows none of the 2024-26 films and only 29% of the candidates the app looked up over eight
  days. Its licence covers research only.
- **An LLM.** Zero-shot LLMs predicted individual ratings no better than each film's average rating on MovieLens
  ([Kang et al. 2023](https://arxiv.org/abs/2305.06474), not peer-reviewed; LLMs did better on books), and the model
  already beats that average. A prompt would also upload my ratings, and LLMs know little about films released after
  their training. Embeddings of titles from a local model helped slightly, but they mostly encode a film's fame,
  which fades for new films.
- **Rating services.** Filmweb, Criticker, Trakt and Letterboxd give no API with predicted ratings and no
  independent evidence of accuracy. Trakt's terms forbid use in apps that promote piracy.

Simple models matching complex ones is a known pattern in recommender research
([Ferrari Dacrema et al. 2019](https://arxiv.org/abs/1907.06902), [Rendle et al. 2020](https://arxiv.org/abs/2005.09683)),
though that research is about choosing from whole catalogues, not ranking a given list.

## What could change this
- **New releases' scores.** Early IMDb ratings tend to start high and fall
  ([IMDb Help Center](https://help.imdb.com/article/imdb/track-movies-tv/why-do-the-user-ratings-on-many-movies-and-tv-shows-seem-to-decrease-over-time/G4SK7DG5EA35B4GF)). Shrinking
  scores with few votes toward the average could correct this, but nothing on disk shows by how much. Logging each
  score the app reads, and the suggestions it shows, would measure it within a few months.
- **Calibration on films I didn't choose.** Every result above comes from films I chose to watch. Logged suggestions
  would show whether the predictions hold for the rest.
- **Series.** Telling the model whether a title is a series gave a small, consistent gain, too small to be sure of.
