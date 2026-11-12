// What To Watch: a ranked poster grid, fed one film at a time by a server-sent event stream.

const grid = document.getElementById('films');
const more = document.getElementById('more');
const status = document.getElementById('status');
const dialog = document.getElementById('details');

const ranked = [];        // {film, page, batch, arrival, card}: batches in loading order, each in the selected order
const seen = new Set();   // lastEventId of every film received
const dividers = new Map(); // batch -> the row that starts it
let sortBy = 'score';
let arrivals = 0;
let lastPage = 0;         // the last page that finished loading
let shown = null;         // the entry open in the dialog
let refocusMore = false;  // whether the footer button had focus before it was replaced
let anchor = null;        // the card kept still while films are inserted, see keepingViewStill
let anchoredAt = 0;       // the scroll position after the last correction

const SERVICES = {
    'primevideo.com': 'Prime Video', 'netflix.com': 'Netflix', 'max.com': 'Max', 'hbomax.com': 'Max',
    'disneyplus.com': 'Disney+', 'skyshowtime.com': 'SkyShowtime', 'canalplus.com': 'Canal+',
    'player.pl': 'Player', 'apple.com': 'Apple TV+',
};

const byScore = (a, b) => b.film.score - a.film.score || a.arrival - b.arrival;
const COMPARE = {
    score: byScore,
    mine: (a, b) => (b.film.predictedRating ?? -1) - (a.film.predictedRating ?? -1) || byScore(a, b),
};

// Each page is sorted on its own and goes below the earlier ones, so loading more never reorders what is
// already on screen. Merging puts every film into one batch, which makes the whole list one ranking again.
const compare = (a, b) => a.batch - b.batch || COMPARE[sortBy](a, b);

const ICON = {
    heart: '<path d="M12 21s-7.5-4.6-9.6-9.2C.9 8.4 3 4.5 6.7 4.5c2.1 0 3.5 1.1 5.3 3 1.8-1.9 3.2-3 5.3-3 3.7 0 5.8 3.9 4.3 7.3C19.5 16.4 12 21 12 21z" fill="currentColor"/>',
    close: '<path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" fill="none"/>',
    prev: '<path d="M15 5l-7 7 7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>',
    next: '<path d="M9 5l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>',
    play: '<path d="M8 5v14l11-7z" fill="currentColor"/>',
};

const percent = fraction => Math.round(fraction * 100);
const count = n => n.toLocaleString('pl-PL');
const filmCount = n => `${n} ${n === 1 ? 'film' : 'films'}`;

// h('p', {class: 'x'}, 'text', child) builds an element; null and false children are skipped.
function h(tag, props = {}, ...children) {
    const el = document.createElement(tag);
    for (const [key, value] of Object.entries(props)) {
        if (value == null || value === false) continue;
        if (key.startsWith('on')) el.addEventListener(key.slice(2), value);
        else el.setAttribute(key, value);
    }
    el.append(...children.flat().filter(child => child != null && child !== false));
    return el;
}

function svg(path) {
    const el = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    el.setAttribute('viewBox', '0 0 24 24');
    el.setAttribute('class', 'icon');
    el.setAttribute('aria-hidden', 'true');
    el.innerHTML = path;
    return el;
}

function serviceName(link) {
    try {
        const host = new URL(link).hostname;
        const domain = Object.keys(SERVICES).find(key => host === key || host.endsWith('.' + key));
        return domain ? SERVICES[domain] : '';
    } catch {
        return '';
    }
}

// A stable hue per title, so a film without a poster always gets the same colour.
function hue(text) {
    let hash = 0;
    for (const char of text) hash = (hash * 31 + char.codePointAt(0)) | 0;
    return Math.abs(hash) % 360;
}

// The title is sized so its longest word fits on one line (bold capitals are about 0.7em wide, the box 78cqi)
// and the whole title fits in the lower half of the poster.
function placeholder(film) {
    const longestWord = Math.max(...film.title.split(/\s+/).map(word => word.length));
    const size = Math.min(16, 78 / (longestWord * .7), Math.sqrt(6000 / film.title.length));
    return h('div', {class: 'placeholder', style: `--hue: ${hue(film.title)}; --size: ${size.toFixed(2)}cqi`, 'aria-hidden': 'true'},
        h('span', {class: 'ph-year'}, film.year),
        h('span', {class: 'ph-title'}, film.title),
        film.genres.length > 0 && h('span', {class: 'ph-genre'}, film.genres[0]));
}

function poster(film) {
    if (!film.poster) return placeholder(film);
    const img = h('img', {src: film.poster, alt: '', loading: 'lazy', decoding: 'async'});
    img.addEventListener('load', () => img.classList.add('loaded'), {once: true});
    img.addEventListener('error', () => img.replaceWith(placeholder(film)), {once: true});
    return img;
}

function scoreBadge(film, cls = 'badge') {
    return h('span', {class: cls, title: `Combined score from ${film.scores.length} sources`},
        h('span', {class: 'visually-hidden'}, 'Combined score '), percent(film.score), h('small', {}, '%'));
}

function predicted(film) {
    if (film.predictedRating == null) return null;
    return h('p', {class: 'predicted'},
        svg(ICON.heart), Math.round(film.predictedRating * 10), '%', h('span', {class: 'for-you'}, 'for you'));
}

function card(entry) {
    const {film} = entry;
    const sources = film.scores.map(score => h('span', {}, score.source, ' ', h('b', {}, percent(score.grade))));
    return h('li', {class: 'film'},
        h('div', {class: 'poster'},
            poster(film),
            scoreBadge(film),
            h('span', {class: 'new-tag'}, 'New'),
            h('div', {class: 'peek', 'aria-hidden': 'true'},
                film.plot && h('p', {class: 'peek-plot'}, film.plot),
                h('p', {class: 'peek-sources'}, sources))),
        h('h2', {class: 'title'},
            h('button', {type: 'button', 'aria-haspopup': 'dialog', onclick: () => showDetails(entry)}, film.title)),
        h('p', {class: 'meta'}, [film.year, serviceName(film.link)].filter(Boolean).join(' · ')),
        predicted(film));
}

function skeletons(number) {
    return Array.from({length: number}, () => h('li', {class: 'film skeleton', 'aria-hidden': 'true'},
        h('div', {class: 'poster'}), h('div', {class: 'line'}), h('div', {class: 'line short'})));
}

// Inserting or moving cards above the reader would push the page under them, so the first card that starts in
// view is kept at the same height on screen. The same card stays the anchor until the reader scrolls: in a grid
// a card drops a row only after several insertions, so a fresh anchor each time would let the page creep down.
// At the very top nothing is held, so new leaders come into view. A moved card loses focus, so it is put back.
function keepingViewStill(change) {
    const focused = grid.contains(document.activeElement) ? document.activeElement : null;
    if (window.scrollY === 0) {
        anchor = null;
    } else if (!anchor || window.scrollY !== anchoredAt) {
        anchor = ranked.find(entry => entry.card.offsetTop >= window.scrollY)?.card;
    }
    const before = anchor?.offsetTop;
    change();
    if (anchor) window.scrollBy(0, anchor.offsetTop - before);
    anchoredAt = window.scrollY;
    if (focused && document.activeElement !== focused) focused.focus({preventScroll: true});
}

function insertionIndex(entry) {
    const index = ranked.findIndex(other => compare(entry, other) < 0);
    return index === -1 ? ranked.length : index;
}

function divider(batch) {
    if (!dividers.has(batch)) {
        dividers.set(batch, h('li', {class: 'batch'}, h('p', {}, h('b', {}, `Page ${batch}`), ', sorted on its own')));
    }
    return dividers.get(batch);
}

// Puts the cards, and a divider before each later batch, in the order of `ranked`. Only nodes that are out of
// place move, so loaded posters and focus survive.
function arrange() {
    const nodes = ranked.flatMap((entry, index) =>
        index > 0 && entry.batch !== ranked[index - 1].batch ? [divider(entry.batch), entry.card] : [entry.card]);
    nodes.forEach((node, index) => {
        if (grid.children[index] !== node) grid.insertBefore(node, grid.children[index] ?? null);
    });
}

function addFilm(film, page) {
    // The first film of a new page takes the "New" tags off the last merge.
    if (!ranked.some(entry => entry.page === page)) {
        grid.querySelectorAll('.is-new').forEach(node => node.classList.remove('is-new'));
    }
    const entry = {film, page, batch: page, arrival: arrivals++};
    entry.card = card(entry);
    keepingViewStill(() => {
        ranked.splice(insertionIndex(entry), 0, entry);
        arrange();
    });
}

function resort() {
    keepingViewStill(() => {
        ranked.sort(compare);
        arrange();
    });
}

// The reader asked for one ranking, so the new top of the list is what they want to see.
// The divider shows which films are new until then; afterwards the "New" tags do.
function mergePages() {
    const first = ranked[0].batch;
    ranked.forEach(entry => entry.card.classList.toggle('is-new', entry.batch !== first));
    ranked.forEach(entry => entry.batch = 0);
    dividers.forEach(node => node.remove());
    dividers.clear();
    ranked.sort(compare);
    arrange();
    window.scrollTo(0, 0);
    grid.focus({preventScroll: true});
    showFooter(`All ${filmCount(ranked.length)} in one ranking.`, moreButton());
}

function setStatus(text, busy) {
    status.textContent = text;
    status.classList.toggle('busy', busy);
}

function button(label, onclick, style = 'button') {
    return h('button', {type: 'button', class: style, onclick}, label);
}

function moreButton() {
    return button('More films', () => loadPage(lastPage + 1));
}

// The last action is the one that loads more, so it gets the focus back after a page loads.
function showFooter(text, ...actions) {
    more.replaceChildren(h('p', {}, text), ...actions);
    if (refocusMore && actions.length > 0) actions.at(-1).focus();
    refocusMore = false;
}

function showLoading(page) {
    refocusMore = more.contains(document.activeElement);
    more.replaceChildren();
    grid.querySelectorAll('.skeleton').forEach(node => node.remove());
    grid.append(...skeletons(ranked.length === 0 ? 16 : 4));
    setStatus(`Loading page ${page}…`, true);
}

// How many of the page's films would be in the top 10 if every page were one ranking.
function topTenFrom(page) {
    return ranked.toSorted(COMPARE[sortBy]).slice(0, 10).filter(entry => entry.page === page).length;
}

function showPageDone(page) {
    const added = ranked.filter(entry => entry.page === page).length;   // counts a retried page as a whole
    grid.querySelectorAll('.skeleton').forEach(node => node.remove());
    setStatus(filmCount(ranked.length), false);
    if (added === 0) {
        showFooter(ranked.length ? 'That’s everything for now.' : 'No suggestions right now.');
        return;
    }
    lastPage = page;
    if (!ranked.some(entry => entry.batch !== ranked[0].batch)) {
        showFooter(`Page ${page} added ${filmCount(added)}.`, moreButton());
        return;
    }
    const top = topTenFrom(page);
    showFooter(`Page ${page} added ${filmCount(added)} below. ${top === 0 ? 'None' : top} of them would make your top 10.`,
        button('Merge into one ranking', mergePages, 'button secondary'), moreButton());
}

function showInterrupted(page) {
    grid.querySelectorAll('.skeleton').forEach(node => node.remove());
    setStatus(`${filmCount(ranked.length)} · connection lost`, false);
    showFooter(`Loading page ${page} stopped before it finished.`, button(`Retry page ${page}`, () => loadPage(page)));
}

function loadPage(page) {
    const source = new EventSource(`/resources/suggestions/${page}/stream`);
    showLoading(page);

    source.addEventListener('film', event => {
        if (seen.has(event.lastEventId)) return;
        seen.add(event.lastEventId);
        addFilm(JSON.parse(event.data), page);
        setStatus(`Loading page ${page}… ${filmCount(ranked.length)}`, true);
    });
    source.addEventListener('poisonPill', () => {
        source.close();
        showPageDone(page);
    });
    source.onerror = () => {
        source.close();
        showInterrupted(page);
    };
}

function sourceRow(score) {
    const critics = score.type === 'CRITIC';
    const unit = critics ? (score.quantity === 1 ? 'critic' : 'critics') : (score.quantity === 1 ? 'vote' : 'votes');
    return h('li', {},
        h('a', {href: score.url, target: '_blank', rel: 'noopener'}, score.source),
        h('span', {class: 'meter', style: `--value: ${percent(score.grade)}%`, 'aria-hidden': 'true'}),
        h('b', {}, percent(score.grade), '%'),
        h('span', {class: 'votes'}, `${count(score.quantity)} ${unit}`));
}

function showDetails(entry) {
    shown = entry;
    const {film} = entry;
    const index = ranked.indexOf(entry);
    const step = delta => ranked[index + delta] && showDetails(ranked[index + delta]);
    const service = serviceName(film.link);

    dialog.replaceChildren(
        h('div', {class: 'details-poster'}, poster(film)),
        h('div', {class: 'details-head'},
            h('p', {class: 'kicker'}, [film.year, ...film.genres].join(' · ')),
            h('h2', {id: 'details-title'}, film.title),
            h('div', {class: 'summary'},
                scoreBadge(film, 'badge big'),
                h('span', {class: 'summary-label'}, 'Combined score', h('br'), `from ${film.scores.length} sources`),
                film.predictedRating != null && h('span', {class: 'summary-mine'},
                    svg(ICON.heart), h('b', {}, Math.round(film.predictedRating * 10), '%'), h('span', {}, 'predicted for you')))),
        h('div', {class: 'details-body'},
            h('p', {class: film.plot ? 'plot' : 'plot missing'}, film.plot || 'No plot summary yet.'),
            h('ul', {class: 'sources', 'aria-label': 'Scores by source'}, film.scores.map(sourceRow)),
            h('a', {class: 'button watch', href: film.link, target: '_blank', rel: 'noopener'},
                svg(ICON.play), service ? `Watch on ${service}` : 'Watch now')),
        h('div', {class: 'details-nav'},
            h('button', {type: 'button', class: 'icon-button', 'aria-label': 'Previous film', disabled: index === 0,
                onclick: () => step(-1)}, svg(ICON.prev)),
            h('span', {}, `${index + 1} / ${ranked.length}`),
            h('button', {type: 'button', class: 'icon-button', 'aria-label': 'Next film', disabled: index === ranked.length - 1,
                onclick: () => step(1)}, svg(ICON.next))),
        h('button', {type: 'button', class: 'icon-button close', 'aria-label': 'Close', onclick: () => dialog.close()}, svg(ICON.close)));
    dialog.onkeydown = event => {
        if (event.key === 'ArrowLeft') step(-1);
        if (event.key === 'ArrowRight') step(1);
    };
    if (!dialog.open) dialog.showModal();
    dialog.querySelector('.close').focus();
}

// Close on a backdrop click. The dialog's own empty areas also report the dialog as target, so check position.
dialog.addEventListener('click', event => {
    const box = dialog.getBoundingClientRect();
    const outside = event.clientX < box.left || event.clientX > box.right
        || event.clientY < box.top || event.clientY > box.bottom;
    if (outside && event.detail > 0) dialog.close();
});
dialog.addEventListener('close', () => shown?.card.querySelector('.title button').focus());

document.querySelector('.sort').addEventListener('change', event => {
    sortBy = event.target.value;
    resort();
});

loadPage(1);
