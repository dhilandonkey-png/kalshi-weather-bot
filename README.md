# kalshi-weather-bot

[![test](https://github.com/dhilandonkey-png/kalshi-weather-bot/actions/workflows/test.yml/badge.svg)](https://github.com/dhilandonkey-png/kalshi-weather-bot/actions/workflows/test.yml)
![Java 21](https://img.shields.io/badge/java-21-blue)
![License: MIT](https://img.shields.io/badge/license-MIT-green)

A bot that paper-trades Kalshi's daily high-temperature markets using National
Weather Service forecasts, and keeps score of whether its forecasts are actually
better than the market's.

**Paper trading only.** It reads Kalshi's public market data and never places a real
order. There's no account, no API key and no real money involved.

Built by [Dhilan Avula](https://dhilanavula.dev), following up on my Kalshi
prediction market case study (Kelly criterion and Brier scores).

## The idea

Kalshi lists markets like *"Will the high in New York on Oct 22 be 71–72°?"*. A YES
contract pays $1 if it happens, so its price is the market's probability. The NWS
publishes a free forecast high for the same station the market settles on. If the
forecast says 72° and the 71–72° market is priced at 10¢, either the market knows
something, or the contract is cheap.

```
NWS forecast high ──► model: P(each range) ──┐
                                             ├──► edge = P − price − fee ──► Kelly size ──► paper trade
Kalshi prices ───────────────────────────────┘
                                                      next day: settle, pay out, grade the forecast
```

A GitHub Action runs it every morning and commits the results, so `REPORT.md` and
`state.json` are a running, public track record.

## How it decides

**1. Probability from the forecast.** The actual high is modeled as a normal
distribution centered on the forecast, with a spread σ (3°F to start). Recorded
highs are whole degrees, so a 71–72° range covers every true temperature from 70.5°
to 72.5°:

```
P(71–72°) = Φ((72.5 − forecast) / σ) − Φ((70.5 − forecast) / σ)
```

A test checks that a full ladder of ranges (below 66, 66–67, …, above 77) always adds
up to 100%, whatever the forecast.

**2. Edge after fees.** Kalshi charges `ceil(0.07 × contracts × price × (1 − price))`.
The bot only trades when `model probability − price − fee` is at least 5¢ per
contract, on whichever side (YES or NO) has more edge.

**3. Size with Kelly.** For a contract that costs P and pays $1 with probability q,
the Kelly criterion says to risk `(q − P) / (1 − P)` of the bankroll. Full Kelly is
reckless when the model is wrong, so the bot bets a quarter of that, capped at 50
contracts per market.

## How it's graded

Profit over a few weeks is mostly luck, so the bot also records the model's
probability **and the market's midpoint price** for every market it looks at, traded
or not. After settlement both get a **Brier score**: the average of
(probability − outcome)², where 0 is perfect and 0.25 is always guessing 50%.

The model only has a real edge if its Brier score beats the market's on the same
markets. That comparison is in `REPORT.md`.

## Status

| | |
|---|---|
| Model, fees, Kelly sizing, paper account, scorecard | ✅ built and tested (32 tests) |
| Kalshi and NWS clients | ✅ built; parsing tested against hand-written examples |
| First live run | ⏳ next: check real responses against the examples |
| Fit σ and bias from results | planned, once there are a few weeks of settled markets |

## Decisions and tradeoffs

- **Paper only.** Kalshi requires users to be 18 or older. Paper trading also makes
  results reproducible: every decision is in git.
- **Money is whole cents in a `long`**, the same as in my
  [safe-payouts](https://github.com/dhilandonkey-png/safe-payouts) project, so fees
  and payouts never pick up floating-point error.
- **One decision per market.** The bot buys once and holds to settlement. Simpler to
  reason about, and it can't churn fees by trading in and out.
- **The first prediction is kept.** Later runs don't overwrite it, so the bot can't
  improve its score by updating a forecast right before the result is known.
- **Fees are estimated per contract when sizing**, rounded up, which slightly
  overstates them. Cautious on purpose: it skips a trade rather than overpaying.
- **Known limits:** σ = 3°F is a starting guess, not fit to data yet, and the normal
  distribution ignores that forecast errors can be lopsided. It buys at the ask
  without considering how many contracts are actually offered there.

## Run it

Needs Java 21. Gradle downloads itself the first time.

```bash
./gradlew test   # all tests, no network needed
./gradlew run    # one live paper-trading run; writes state.json and REPORT.md
```

To track another city, add its Kalshi series ticker and the coordinates of the
weather station it settles on to [`City.java`](src/main/java/weatherbot/City.java).
