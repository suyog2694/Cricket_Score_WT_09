import { useEffect, useState } from 'react'
import {
  Activity, ArrowDownUp, ArrowRight, BadgeCheck, CircleHelp, Clock3,
  CloudOff, Plus, RefreshCw, Shield, SquarePen, Users, X,
} from 'lucide-react'

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
const EXTRA_TYPES = ['NONE', 'WIDE', 'NO_BALL', 'BYE', 'LEG_BYE']
const ROLE_OPTIONS = ['Batter', 'Bowler', 'All-rounder', 'Wicketkeeper']

async function api(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.detail || body?.message || `Request failed (${response.status})`)
  }
  return response.status === 204 ? null : response.json()
}

function initials(name = '') {
  return name.split(' ').slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

function App() {
  const [section, setSection] = useState('live')
  const [matches, setMatches] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [score, setScore] = useState(null)
  const [teams, setTeams] = useState([])
  const [squads, setSquads] = useState({})
  const [manageTeamId, setManageTeamId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [showMatchForm, setShowMatchForm] = useState(false)
  const [delivery, setDelivery] = useState({ strikerId: '', nonStrikerId: '', bowlerId: '', batterRuns: 0, extras: 1, extraType: 'NONE', wicket: false, dismissalType: 'Bowled' })
  const [teamForm, setTeamForm] = useState({ name: '', shortName: '' })
  const [playerForm, setPlayerForm] = useState({ name: '', role: 'Batter', jerseyNumber: '' })

  async function refresh(preferredId = selectedId, quiet = false) {
    if (!quiet) setIsRefreshing(true)
    try {
      const [matchRows, teamRows] = await Promise.all([api('/matches'), api('/teams')])
      setMatches(matchRows)
      setTeams(teamRows)
      const chosen = matchRows.find((match) => match.id === Number(preferredId)) || matchRows[0] || null
      setSelectedId(chosen?.id ?? null)
      setScore(chosen)
      setError('')
    } catch (problem) {
      setError(problem.message || 'Could not connect to the score service.')
    } finally {
      setLoading(false)
      setIsRefreshing(false)
    }
  }

  useEffect(() => {
    refresh(null, true)
    const timer = window.setInterval(() => refresh(selectedId, true), 5000)
    return () => window.clearInterval(timer)
  }, [selectedId])

  useEffect(() => {
    if (!score?.battingTeam?.id || !score?.bowlingTeam?.id) return
    Promise.all([
      api(`/teams/${score.battingTeam.id}/players`),
      api(`/teams/${score.bowlingTeam.id}/players`),
    ]).then(([batting, bowling]) => {
      setSquads((current) => ({ ...current, [score.battingTeam.id]: batting, [score.bowlingTeam.id]: bowling }))
      setDelivery((current) => ({
        ...current,
        strikerId: batting[0]?.id || '',
        nonStrikerId: batting[1]?.id || '',
        bowlerId: bowling[0]?.id || '',
      }))
    }).catch((problem) => setError(problem.message))
  }, [score?.battingTeam?.id, score?.bowlingTeam?.id])

  useEffect(() => {
    if (!manageTeamId) return
    api(`/teams/${manageTeamId}/players`)
      .then((players) => setSquads((current) => ({ ...current, [manageTeamId]: players })))
      .catch((problem) => setError(problem.message))
  }, [manageTeamId])

  async function recordDelivery(event) {
    event.preventDefault()
    try {
      const updated = await api(`/matches/${selectedId}/events`, {
        method: 'POST',
        body: JSON.stringify({
          ...delivery,
          strikerId: Number(delivery.strikerId),
          nonStrikerId: delivery.nonStrikerId ? Number(delivery.nonStrikerId) : null,
          bowlerId: Number(delivery.bowlerId),
          batterRuns: Number(delivery.batterRuns),
          extras: delivery.extraType === 'NONE' ? 0 : Number(delivery.extras),
          wicketPlayerId: delivery.wicket ? Number(delivery.strikerId) : null,
        }),
      })
      setScore(updated)
      setMatches((current) => current.map((match) => match.id === updated.id ? updated : match))
      setNotice('Delivery saved to the match log.')
      window.setTimeout(() => setNotice(''), 2600)
    } catch (problem) {
      setError(problem.message)
    }
  }

  async function saveTeam(event) {
    event.preventDefault()
    try {
      const team = await api('/teams', { method: 'POST', body: JSON.stringify(teamForm) })
      setTeams((current) => [...current, team])
      setManageTeamId(String(team.id))
      setTeamForm({ name: '', shortName: '' })
      setNotice(`${team.name} added to your teams.`)
    } catch (problem) { setError(problem.message) }
  }

  async function savePlayer(event) {
    event.preventDefault()
    try {
      const player = await api(`/teams/${manageTeamId}/players`, {
        method: 'POST',
        body: JSON.stringify({ ...playerForm, jerseyNumber: playerForm.jerseyNumber ? Number(playerForm.jerseyNumber) : null }),
      })
      setSquads((current) => ({ ...current, [manageTeamId]: [...(current[manageTeamId] || []), player] }))
      setPlayerForm({ name: '', role: 'Batter', jerseyNumber: '' })
      setNotice(`${player.name} added to the squad.`)
    } catch (problem) { setError(problem.message) }
  }

  async function startNextInnings() {
    try {
      const endpoint = score.currentInnings === 1 ? 'innings/next' : 'finish'
      const updated = await api(`/matches/${selectedId}/${endpoint}`, { method: 'POST' })
      setScore(updated)
      setMatches((current) => current.map((match) => match.id === updated.id ? updated : match))
    } catch (problem) { setError(problem.message) }
  }

  const battingPlayers = squads[score?.battingTeam?.id] || []
  const bowlingPlayers = squads[score?.bowlingTeam?.id] || []
  const managedPlayers = squads[manageTeamId] || []
  const liveMatches = matches.filter((match) => match.status === 'LIVE')

  return (
    <div className="app-shell">
      <aside className="rail">
        <div className="brand-lockup"><span className="brand-mark"><Activity size={19} strokeWidth={2.8} /></span><span>BOUNDARY<span className="brand-period">.</span></span></div>
        <div className="rail-label">WORKSPACE</div>
        <nav className="primary-nav" aria-label="Main navigation">
          <button className={section === 'live' ? 'nav-item active' : 'nav-item'} onClick={() => setSection('live')}><Activity size={17} />Live desk</button>
          <button className={section === 'teams' ? 'nav-item active' : 'nav-item'} onClick={() => setSection('teams')}><Users size={17} />Teams & players</button>
        </nav>
        <div className="rail-section-head"><span className="rail-label">MATCH CENTRE</span><button className="icon-button rail-plus" title="Create a match" onClick={() => setShowMatchForm(true)}><Plus size={16} /></button></div>
        <div className="match-list">
          {matches.map((match) => (
            <button key={match.id} className={`match-link ${selectedId === match.id ? 'selected' : ''}`} onClick={() => { setSelectedId(match.id); setScore(match) }}>
              <span className={`match-link-dot ${match.status === 'LIVE' ? 'is-live' : ''}`} />
              <span className="match-link-copy"><strong>{match.teamA.shortName} <i>v</i> {match.teamB.shortName}</strong><small>{match.status === 'LIVE' ? `${match.runs}/${match.wickets} · ${match.overs} ov` : match.status}</small></span>
              <ArrowRight size={14} className="match-link-arrow" />
            </button>
          ))}
          {!matches.length && <p className="rail-empty">No matches yet.</p>}
        </div>
        <div className="rail-bottom"><span className="connection-light" /><span>Score service</span><strong>{error ? 'Offline' : loading ? 'Checking' : 'Connected'}</strong></div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb"><span>BOUNDARY</span><span className="crumb-slash">/</span><strong>{section === 'live' ? 'LIVE DESK' : 'TEAM MANAGEMENT'}</strong></div>
          <div className="top-actions"><span className="live-count"><span />{liveMatches.length} LIVE</span><button className="icon-button refresh-button" title="Refresh scores" onClick={() => refresh()}><RefreshCw size={16} className={isRefreshing ? 'spinning' : ''} /></button><div className="operator"><span className="operator-avatar">SC</span><span>Scorekeeper</span></div></div>
        </header>

        {error && <div className="alert error-alert"><CloudOff size={17} /><span>{error.includes('Failed to fetch') ? 'Cannot reach the API. Start Spring Boot and check the database connection.' : error}</span><button className="icon-button" onClick={() => setError('')} aria-label="Dismiss"><X size={15} /></button></div>}
        {notice && <div className="alert success-alert"><BadgeCheck size={17} /><span>{notice}</span></div>}

        {section === 'live' ? (
          <div className="page-content">
            <div className="page-heading">
              <div><div className="eyebrow"><span className="eyebrow-line" />MATCH OPERATIONS</div><h1>Live desk<span className="title-dot">.</span></h1><p>Every delivery, every detail. Updated in real time.</p></div>
              <div className="heading-actions"><span className="sync-label"><span className="sync-pulse" />AUTO SYNC · 5 SEC</span><button className="button button-dark" onClick={() => setShowMatchForm(true)}><Plus size={16} />New match</button></div>
            </div>

            {loading ? <div className="empty-state">Connecting to match centre…</div> : score ? <>
              <section className="score-hero" aria-label="Current match score">
                <div className="score-hero-top"><div className="live-pill"><span />{score.status}</div><span className="match-meta">{score.title} <span>·</span> {score.venue || 'Venue not set'}</span><span className="format-label">T20 · MATCH {String(score.id).padStart(2, '0')}</span></div>
                <div className="score-main-row">
                  <div className="team-score"><span className="team-monogram">{score.battingTeam.shortName.slice(0, 1)}</span><div><div className="team-name">{score.battingTeam.name}</div><div className="team-context">BATTING · INNINGS {score.currentInnings}</div></div></div>
                  <div className="score-numbers"><strong>{score.runs}<span>/</span>{score.wickets}</strong><span className="overs-display">{score.overs}<i> / {score.oversLimit} ov</i></span></div>
                  <div className="opposition-score"><div className="opposition-label">{score.bowlingTeam.shortName}<span> · BOWLING</span></div><div className="opposition-name">{score.bowlingTeam.name}</div><div className="innings-tag">{score.target ? `TARGET ${score.target}` : '1ST INNINGS'}</div></div>
                </div>
                <div className="score-hero-footer"><div className="score-fact"><span>RUN RATE</span><strong>{Number(score.runRate).toFixed(2)}</strong></div>{score.target && <div className="score-fact"><span>REQUIRED RATE</span><strong>{Number(score.requiredRunRate).toFixed(2)}</strong></div>}<div className="score-fact"><span>OVERS LEFT</span><strong>{Math.max(0, score.oversLimit * 6 - score.legalBalls) > 0 ? `${Math.floor((score.oversLimit * 6 - score.legalBalls) / 6)}.${(score.oversLimit * 6 - score.legalBalls) % 6}` : '0.0'}</strong></div><div className="score-fact venue-fact"><span>GROUND</span><strong>{score.venue || 'Not set'}</strong></div><button className="innings-button" onClick={startNextInnings} disabled={score.status === 'COMPLETED'}><ArrowDownUp size={15} />{score.currentInnings === 1 ? 'Start 2nd innings' : 'Finish match'}</button></div>
              </section>

              <div className="stats-strip">
                <div className="stat-cell"><span className="stat-icon orange"><Activity size={17} /></span><div><small>RUN RATE</small><strong>{Number(score.runRate).toFixed(2)}</strong></div><span className="stat-note">per over</span></div>
                <div className="stat-cell"><span className="stat-icon lime"><Clock3 size={17} /></span><div><small>LEGAL BALLS</small><strong>{score.legalBalls}</strong></div><span className="stat-note">this innings</span></div>
                <div className="stat-cell"><span className="stat-icon blue"><Shield size={17} /></span><div><small>WICKETS</small><strong>{score.wickets}<i> / 10</i></strong></div><span className="stat-note">down</span></div>
                <div className="stat-cell"><span className="stat-icon coral"><Users size={17} /></span><div><small>INNINGS</small><strong>{score.currentInnings}<i> / 2</i></strong></div><span className="stat-note">{score.oversLimit} overs</span></div>
              </div>

              <div className="dashboard-grid">
                <div className="left-stack">
                  <section className="panel scorecard-panel">
                    <div className="panel-heading"><div><div className="panel-kicker">THE NUMBERS</div><h2>Current innings</h2></div><span className="panel-caption">{score.battingTeam.shortName} BATTING</span></div>
                    <div className="table-scroll"><table><thead><tr><th>BATTER</th><th>R</th><th>B</th><th>4s</th><th>6s</th><th>SR</th></tr></thead><tbody>
                      {score.batters.map((batter) => <tr key={batter.id}><td><span className="player-cell"><span className="player-avatar">{initials(batter.name)}</span><span>{batter.name}{batter.onStrike && <b className="strike-mark">*</b>}</span></span></td><td className="number-cell strong-cell">{batter.runs}</td><td className="number-cell">{batter.balls}</td><td className="number-cell">{batter.fours}</td><td className="number-cell">{batter.sixes}</td><td className="number-cell">{Number(batter.strikeRate).toFixed(1)}</td></tr>)}
                      {!score.batters.length && <tr><td colSpan="6" className="table-empty">No deliveries recorded yet.</td></tr>}
                    </tbody></table></div>
                    <div className="subtable-heading"><div><div className="panel-kicker">IN THE ATTACK</div><h3>Bowling figures</h3></div><span className="panel-caption">{score.bowlingTeam.shortName}</span></div>
                    <div className="table-scroll"><table><thead><tr><th>BOWLER</th><th>O</th><th>R</th><th>W</th><th>ECON</th></tr></thead><tbody>
                      {score.bowlers.map((bowler) => <tr key={bowler.id}><td><span className="player-cell"><span className="bowler-avatar">{initials(bowler.name)}</span><span>{bowler.name}</span></span></td><td className="number-cell">{bowler.overs}</td><td className="number-cell">{bowler.runs}</td><td className="number-cell strong-cell">{bowler.wickets}</td><td className="number-cell">{Number(bowler.economy).toFixed(1)}</td></tr>)}
                      {!score.bowlers.length && <tr><td colSpan="5" className="table-empty">No bowling figures yet.</td></tr>}
                    </tbody></table></div>
                  </section>

                  <section className="panel event-panel">
                    <div className="panel-heading"><div><div className="panel-kicker">BALL BY BALL</div><h2>Recent events</h2></div><span className="panel-caption">LATEST FIRST</span></div>
                    <div className="event-list">
                      {score.recentEvents.map((item, index) => <div className="event-row" key={item.id}><span className={`event-over ${index === 0 ? 'current-over' : ''}`}>{item.overLabel}</span><span className={`event-token ${item.wicket ? 'wicket-token' : item.runs === 4 || item.runs === 6 ? 'boundary-token' : ''}`}>{item.wicket ? 'W' : item.runs}</span><span className="event-description">{item.description}</span><span className="event-time">{new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span></div>)}
                      {!score.recentEvents.length && <div className="table-empty">Your first ball is waiting to be scored.</div>}
                    </div>
                  </section>
                </div>

                <div className="right-stack">
                  <section className="panel scorer-panel">
                    <div className="panel-heading"><div><div className="panel-kicker">LIVE INPUT</div><h2>Record a ball</h2></div><span className="scorer-tag"><SquarePen size={13} />SCORER</span></div>
                    <form onSubmit={recordDelivery}>
                      <label className="field-label">STRIKER<select required value={delivery.strikerId} onChange={(event) => setDelivery({ ...delivery, strikerId: event.target.value })}><option value="">Select batter</option>{battingPlayers.map((player) => <option key={player.id} value={player.id}>{player.name}</option>)}</select></label>
                      <label className="field-label">NON-STRIKER<select value={delivery.nonStrikerId} onChange={(event) => setDelivery({ ...delivery, nonStrikerId: event.target.value })}><option value="">Select batter</option>{battingPlayers.map((player) => <option key={player.id} value={player.id}>{player.name}</option>)}</select></label>
                      <label className="field-label">BOWLER<select required value={delivery.bowlerId} onChange={(event) => setDelivery({ ...delivery, bowlerId: event.target.value })}><option value="">Select bowler</option>{bowlingPlayers.map((player) => <option key={player.id} value={player.id}>{player.name}</option>)}</select></label>
                      <div className="field-label">BATTER RUNS<div className="run-picker">{[0, 1, 2, 3, 4, 6].map((runs) => <button type="button" key={runs} className={Number(delivery.batterRuns) === runs ? 'run-option selected' : 'run-option'} onClick={() => setDelivery({ ...delivery, batterRuns: runs })}>{runs === 0 ? '·' : runs}</button>)}</div></div>
                      <div className="form-split"><label className="field-label">EXTRA TYPE<select value={delivery.extraType} onChange={(event) => setDelivery({ ...delivery, extraType: event.target.value })}>{EXTRA_TYPES.map((type) => <option key={type} value={type}>{type.replace('_', ' ')}</option>)}</select></label><label className="field-label">EXTRAS<input type="number" min="0" value={delivery.extraType === 'NONE' ? 0 : delivery.extras} onChange={(event) => setDelivery({ ...delivery, extras: event.target.value })} disabled={delivery.extraType === 'NONE'} /></label></div>
                      <div className="scorer-footer"><label className="wicket-toggle"><input type="checkbox" checked={delivery.wicket} onChange={(event) => setDelivery({ ...delivery, wicket: event.target.checked })} /><span className="custom-check" /><span>Wicket</span></label>{delivery.wicket && <select className="dismissal-select" value={delivery.dismissalType} onChange={(event) => setDelivery({ ...delivery, dismissalType: event.target.value })}><option>Bowled</option><option>Caught</option><option>LBW</option><option>Run out</option><option>Stumped</option></select>}<button className="button button-lime submit-ball" type="submit" disabled={score.status !== 'LIVE' || !battingPlayers.length || !bowlingPlayers.length}>Save ball <ArrowRight size={15} /></button></div>
                    </form>
                  </section>
                  <section className="panel innings-panel"><div className="panel-heading"><div><div className="panel-kicker">MATCH STORY</div><h2>Innings summary</h2></div><CircleHelp size={17} className="muted-icon" /></div>
                    {score.innings.length ? <div className="innings-list">{score.innings.map((innings) => <div className="innings-row" key={innings.number}><span className="innings-number">0{innings.number}</span><span className="innings-team">{innings.team}</span><span className="innings-overs">{innings.overs} ov</span><strong>{innings.runs}<i>/{innings.wickets}</i></strong></div>)}</div> : <div className="summary-empty">The first innings summary will appear after a ball is recorded.</div>}
                    <div className="match-footnote"><Clock3 size={14} /><span>Score refreshes automatically every 5 seconds</span></div>
                  </section>
                </div>
              </div>
            </> : <div className="empty-state no-match"><span className="empty-ball"><Activity size={22} /></span><h2>No match on the board</h2><p>Create a match to start recording live scores.</p><button className="button button-dark" onClick={() => setShowMatchForm(true)}><Plus size={16} />Create match</button></div>}
            <footer className="page-footer"><span>BOUNDARY LIVE DESK</span><span>MADE FOR THE MOMENT BETWEEN BALLS</span><button onClick={() => setSection('teams')}><Users size={13} /> Manage squads</button></footer>
          </div>
        ) : (
          <div className="page-content teams-page">
            <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-line" />ROSTER MANAGEMENT</div><h1>Teams & players<span className="title-dot">.</span></h1><p>Keep your squads match-ready.</p></div><div className="heading-actions"><span className="roster-count"><Users size={15} />{teams.length} TEAMS</span></div></div>
            <div className="team-management-grid">
              <section className="panel team-form-panel"><div className="panel-heading"><div><div className="panel-kicker">BUILD YOUR LINE-UP</div><h2>Add a team</h2></div><span className="stat-icon orange"><Shield size={17} /></span></div><form onSubmit={saveTeam} className="management-form"><label className="field-label">TEAM NAME<input required value={teamForm.name} onChange={(event) => setTeamForm({ ...teamForm, name: event.target.value })} placeholder="e.g. Mumbai Falcons" /></label><label className="field-label">SHORT NAME<input required maxLength="5" value={teamForm.shortName} onChange={(event) => setTeamForm({ ...teamForm, shortName: event.target.value.toUpperCase() })} placeholder="e.g. MUF" /></label><button className="button button-dark" type="submit"><Plus size={16} />Add team</button></form></section>
              <section className="panel squad-panel"><div className="panel-heading"><div><div className="panel-kicker">SQUAD ROSTER</div><h2>Players</h2></div><span className="roster-count"><Users size={14} />{managedPlayers.length} PLAYERS</span></div>
                <label className="field-label team-select-label">SELECT TEAM<select value={manageTeamId} onChange={(event) => setManageTeamId(event.target.value)}><option value="">Choose a team</option>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label>
                {manageTeamId && <><form onSubmit={savePlayer} className="player-add-form"><input required value={playerForm.name} onChange={(event) => setPlayerForm({ ...playerForm, name: event.target.value })} placeholder="Player name" aria-label="Player name" /><select value={playerForm.role} onChange={(event) => setPlayerForm({ ...playerForm, role: event.target.value })} aria-label="Player role">{ROLE_OPTIONS.map((role) => <option key={role}>{role}</option>)}</select><input type="number" min="1" max="999" value={playerForm.jerseyNumber} onChange={(event) => setPlayerForm({ ...playerForm, jerseyNumber: event.target.value })} placeholder="#" aria-label="Jersey number" /><button className="button button-lime" type="submit"><Plus size={15} />Add player</button></form><div className="roster-list">{managedPlayers.map((player, index) => <div className="roster-row" key={player.id}><span className="roster-index">{String(index + 1).padStart(2, '0')}</span><span className="player-avatar">{initials(player.name)}</span><span className="roster-name">{player.name}<small>{player.role}</small></span><span className="jersey-number">{player.jerseyNumber ? `#${player.jerseyNumber}` : '—'}</span></div>)}{!managedPlayers.length && <div className="table-empty">No players in this squad yet.</div>}</div></>}
              </section>
            </div>
            <footer className="page-footer"><span>BOUNDARY ROSTER DESK</span><span>ROSTERS ARE STORED WITH YOUR MATCH DATA</span><button onClick={() => setSection('live')}><Activity size={13} />Back to live desk</button></footer>
          </div>
        )}
      </main>

      {showMatchForm && <NewMatchModal teams={teams} onClose={() => setShowMatchForm(false)} onCreate={async (values) => {
        try {
          const created = await api('/matches', { method: 'POST', body: JSON.stringify(values) })
          setMatches((current) => [created, ...current])
          setSelectedId(created.id)
          setScore(created)
          setSection('live')
          setShowMatchForm(false)
          setNotice('Match created. Add squads and start scoring.')
        } catch (problem) { setError(problem.message) }
      }} />}
    </div>
  )
}

function NewMatchModal({ teams, onClose, onCreate }) {
  const [form, setForm] = useState({ title: '', venue: '', oversLimit: 20, teamAId: '', teamBId: '', battingFirstTeamId: '' })
  function submit(event) {
    event.preventDefault()
    onCreate({ ...form, oversLimit: Number(form.oversLimit), teamAId: Number(form.teamAId), teamBId: Number(form.teamBId), battingFirstTeamId: Number(form.battingFirstTeamId || form.teamAId) })
  }
  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}><section className="modal" role="dialog" aria-modal="true" aria-labelledby="new-match-title"><div className="modal-header"><div><div className="panel-kicker">NEW FIXTURE</div><h2 id="new-match-title">Create a match</h2></div><button className="icon-button" onClick={onClose} aria-label="Close"><X size={18} /></button></div><form onSubmit={submit} className="modal-form"><label className="field-label">MATCH TITLE<input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="Optional match label" /></label><label className="field-label">VENUE<input value={form.venue} onChange={(event) => setForm({ ...form, venue: event.target.value })} placeholder="Ground or stadium" /></label><div className="form-split"><label className="field-label">TEAM A<select required value={form.teamAId} onChange={(event) => setForm({ ...form, teamAId: event.target.value })}><option value="">Select team</option>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label><label className="field-label">TEAM B<select required value={form.teamBId} onChange={(event) => setForm({ ...form, teamBId: event.target.value })}><option value="">Select team</option>{teams.filter((team) => String(team.id) !== form.teamAId).map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label></div><div className="form-split"><label className="field-label">OVERS<input type="number" min="1" max="50" required value={form.oversLimit} onChange={(event) => setForm({ ...form, oversLimit: event.target.value })} /></label><label className="field-label">BAT FIRST<select value={form.battingFirstTeamId || form.teamAId} onChange={(event) => setForm({ ...form, battingFirstTeamId: event.target.value })}><option value={form.teamAId}>Team A</option>{form.teamBId && <option value={form.teamBId}>Team B</option>}</select></label></div><div className="modal-actions"><button type="button" className="button button-quiet" onClick={onClose}>Cancel</button><button type="submit" className="button button-lime" disabled={!teams.length || !form.teamAId || !form.teamBId || form.teamAId === form.teamBId}><Plus size={15} />Create match</button></div>{!teams.length && <p className="modal-help">Add at least two teams before creating a match.</p>}</form></section></div>
}

export default App