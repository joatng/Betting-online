package pl.coderslab.serviceImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pl.coderslab.model.Country;
import pl.coderslab.model.Event;
import pl.coderslab.model.GoalScorer;
import pl.coderslab.model.League;
import pl.coderslab.model.Player;
import pl.coderslab.repositories.EventRepository;
import pl.coderslab.repositories.GoalScorerRepository;
import pl.coderslab.repositories.PlayerRepository;
import pl.coderslab.service.EventService;
import pl.coderslab.service.LeagueService;

@Service
public class EventServiceImpl implements EventService {

	final String beginingUrl = "https://apifootball.com/api/?action=get_events&from=";
	final String toUrl = "&to=";
	final String APIUrl = "&APIkey=69e25fed4be4381276cb4d5f30e7b2a66a53c71a3f62dcac640e2c1d69f8d1c1";

	@Autowired
	PlayerRepository playerRepo;
	@Autowired
	GoalScorerRepository goalScorerRepo;
	@Autowired
	EventRepository eventRepo;
	@Autowired
	LeagueService leagueService;

	@Override
	public void createEvents(String from, String to) {
		// Completing URL
		String finalUrl = beginingUrl + from + toUrl + to + APIUrl;
		// JSON parser creation:
		JSONParser parser = new JSONParser();

		try {
			BufferedReader in = getJSONReader(finalUrl);
			String inputLine;

			// Reading JSON
			while ((inputLine = in.readLine()) != null) {
				// parsing Array of JSON
				JSONArray JSONEvents = (JSONArray) parser.parse(inputLine);

				// parsing every object
				for (Object jsonEvent : JSONEvents) {

					// Creating JSON object
					JSONObject eventJson = (JSONObject) jsonEvent;

					// creating new event

					Event event = new Event();
					// Id
					Long eventId = Long.parseLong((String) eventJson.get("match_id"));
					event.setId(eventId);

					// League
					Long leagueId = Long.parseLong((String) eventJson.get("league_id"));
					League eventsLeague = leagueService.findById(leagueId);
					event.setLegaue(eventsLeague);

					// Country
					Country country = eventsLeague.getCountry();
					event.setCountry(country);

					// Formatting date from String
					String dateInString = (String) eventJson.get("match_date");
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					LocalDate eventDate = LocalDate.parse(dateInString, formatter);
					event.setDate(eventDate);

					// Event status = "FT" - ended, "HF" - half-time or in format /d/d' -> minutes
					// in the game
					event.setStatus((String) eventJson.get("match_status"));

					// Time of the event
					event.setTime((String) eventJson.get("match_time"));

					event.setHomeTeamName((String) eventJson.get("match_hometeam_name"));

					// In try in case of mistakes in API
					try {
						if (!eventJson.get("match_hometeam_score").toString().equals("")) {
							event.setHomeTeamScore(Integer.parseInt((String) eventJson.get("match_hometeam_score")));
						}

					} catch (Exception e) {
						System.out.println("yes it works");
					}

					// Away Team Name
					event.setAwayTeamName((String) eventJson.get("match_awayteam_name"));

					// In try in case of mistakes in API
					try {
						if (!eventJson.get("match_awayteam_score").toString().equals("")) {
							event.setAwayTeamScore(Integer.parseInt((String) eventJson.get("match_awayteam_score")));
						}
					} catch (Exception e) {
					}
					// In try in case of mistakes in API
					try {
						if (!eventJson.get("match_hometeam_halftime_score").toString().equals("")) {
							event.setHomeTeamScoreHalfTime(
									Integer.parseInt((String) eventJson.get("match_hometeam_halftime_score")));
						}
					} catch (Exception e) {
					}
					// In try in case of mistakes in API
					try {
						if (!eventJson.get("match_awayteam_halftime_score").toString().equals("")) {
							event.setAwayTeamScoreHalfTime(
									Integer.parseInt((String) eventJson.get("match_awayteam_halftime_score")));
						}
					} catch (Exception e) {
					}

					// Is it live?
					event.setMatchLive((String) eventJson.get("match_live"));

					JSONArray jsonArraygoals = (JSONArray) eventJson.get("goalscorer");
					List<GoalScorer> goalScorers = createGoalScorersForTheEvent(jsonArraygoals, eventId);

					// save event
					event = eventRepo.save(event);

					// Save goals
					saveGoalScorers(goalScorers, eventId);

				}
			}
			in.close();
		} catch (

		MalformedURLException e) {
			e.printStackTrace();
			System.out.println("here1");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("here2");
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("here3");
		}

	}

	@Override
	public List<Event> liveEvent() {
		List<Event> liveEvents = new ArrayList<>();
		LocalDate today = LocalDate.now();
		String date = today.toString();
		String url = beginingUrl + date + toUrl + date + APIUrl;
		JSONParser parser = new JSONParser();
		try {
			BufferedReader in = getJSONReader(url);
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				JSONArray jsonEvent = (JSONArray) parser.parse(inputLine);

				for (Object jsonEvenObject : jsonEvent) {

					JSONObject eventJson = (JSONObject) jsonEvenObject;
					String matchId = (String) eventJson.get("match_id");
					Long id = Long.parseLong(matchId);
					Event event = eventRepo.findOne(id);
					long leaugueid = Long.parseLong((String) eventJson.get("league_id"));
					League league = leagueService.findById(leaugueid);
					event.setLegaue(league);
					event.setCountry(league.getCountry());
					event.setStatus((String) eventJson.get("match_status"));
					event.setTime((String) eventJson.get("match_time"));
					event.setHomeTeamName((String) eventJson.get("match_hometeam_name"));

					try {
						if (eventJson.get("match_hometeam_score").equals("")) {
							event.setHomeTeamScore(0);
						} else {
							event.setHomeTeamScore(Integer.parseInt((String) eventJson.get("match_hometeam_score")));
						}
					} catch (Exception e) {
					}

					event.setAwayTeamName((String) eventJson.get("match_awayteam_name"));

					try {
						if (eventJson.get("match_awayteam_score").equals("")) {
							event.setAwayTeamScore(0);
						} else {
							event.setAwayTeamScore(Integer.parseInt((String) eventJson.get("match_awayteam_score")));
						}

					} catch (Exception e) {
					}

					try {
						event.setHomeTeamScoreHalfTime(
								Integer.parseInt((String) eventJson.get("match_hometeam_halftime_score")));
					} catch (Exception e) {
					}
					try {
						event.setAwayTeamScoreHalfTime(
								Integer.parseInt((String) eventJson.get("match_awayteam_halftime_score")));
					} catch (Exception e) {
					}

					event.setMatchLive((String) eventJson.get("match_live"));

					JSONArray jsonArraygoals = (JSONArray) eventJson.get("goalscorer");
					List<GoalScorer> goalScorers = createGoalScorersForTheEvent(jsonArraygoals, id);

					eventRepo.save(event);
					saveGoalScorers(goalScorers, id);

					liveEvents.add(event);

				}
			}
			in.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.out.println("here1");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("here2");
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("here3");
		}

		return liveEvents;
	}

	@Override
	public List<Event> findByDate(LocalDate date) {
		return eventRepo.findAllByDate(date);
	}

	// Method for creating JSON Reader
	private BufferedReader getJSONReader(String url) throws IOException {
		URL getDataFrom = new URL(url);
		URLConnection urlConn = getDataFrom.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
		return in;
	}

	// Method for creating goalScorers
	private List<GoalScorer> createGoalScorersForTheEvent(JSONArray jsonArraygoals, long eventId) {
		List<GoalScorer> goalScorers = new ArrayList<>();
		for (Object jsonGoalScorer : jsonArraygoals) {
			JSONObject goalJson = (JSONObject) jsonGoalScorer;
			GoalScorer goalScorer = new GoalScorer();

			// Time of goal
			goalScorer.setTime((String) goalJson.get("time"));
			// Goal id = event id + time of goal
			goalScorer.setId(eventId + goalScorer.getTime());

			// Two homeScorers names but one is empty
			String homeScorer = (String) goalJson.get("home_scorer");
			String awayScorer = (String) goalJson.get("away_scorer");

			// Matching player with score
			if (!homeScorer.equals("")) {
				Player player = checkIfExistsIfNotCreateNew(homeScorer);
				goalScorer.setHomeScorer(player);
			}
			if (!awayScorer.equals("")) {
				Player player = checkIfExistsIfNotCreateNew(awayScorer);
				goalScorer.setAwayScorer(player);
			}

			// Finally setting score
			goalScorer.setScore((String) goalJson.get("score"));
			goalScorers.add(goalScorer);
		}
		return goalScorers;

	}

	private Player checkIfExistsIfNotCreateNew(String playerName) {
		Player player = null;
		try {
			player = playerRepo.findByName(playerName);
		} catch (Exception e) {
		}
		if (player == null) {
			player = new Player();
			player.setName(playerName);
			player = playerRepo.save(player);
		}
		return player;
	}

	private void saveGoalScorers(List<GoalScorer> goalScorers, long eventId) {
		for (GoalScorer gs : goalScorers) {
			gs.setEvent(eventRepo.findOne(eventId));
		}
		goalScorerRepo.save(goalScorers);
	}

	@Override
	public List<Event> findByDateBetween(LocalDate from, LocalDate to) {
		return eventRepo.findByDateBetween(from, to);
	}

	@Override
	public void updateliveEvents() {

		LocalDate today = LocalDate.now();
		String date = today.toString();
		String url = beginingUrl + date + toUrl + date + APIUrl;
		JSONParser parser = new JSONParser();

		try {
			BufferedReader in = getJSONReader(url);
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				JSONArray jsonEvent = (JSONArray) parser.parse(inputLine);

				for (Object jsonEvenObject : jsonEvent) {

					JSONObject eventJson = (JSONObject) jsonEvenObject;

					String eventId = (String) eventJson.get("match_id");
					Long id = Long.parseLong(eventId);

					Event event = eventRepo.findOne(id);

					event.setStatus((String) eventJson.get("match_status"));

					event.setTime((String) eventJson.get("match_time"));

					try {
						if (eventJson.get("match_hometeam_score").equals("")) {
							event.setHomeTeamScore(0);
						} else {
							event.setHomeTeamScore(Integer.parseInt((String) eventJson.get("match_hometeam_score")));
						}
					} catch (Exception e) {
					}

					try {
						if (eventJson.get("match_awayteam_score").equals("")) {
							event.setAwayTeamScore(0);
						} else {
							event.setAwayTeamScore(Integer.parseInt((String) eventJson.get("match_awayteam_score")));
						}

					} catch (Exception e) {
					}

					try {
						event.setHomeTeamScoreHalfTime(
								Integer.parseInt((String) eventJson.get("match_hometeam_halftime_score")));
					} catch (Exception e) {
					}
					try {
						event.setAwayTeamScoreHalfTime(
								Integer.parseInt((String) eventJson.get("match_awayteam_halftime_score")));
					} catch (Exception e) {
					}
					JSONArray jsonArraygoals = (JSONArray) eventJson.get("goalscorer");
					List<GoalScorer> goalScorers = createGoalScorersForTheEvent(jsonArraygoals, id);
					eventRepo.save(event);
					saveGoalScorers(goalScorers, id);

				}

			}
			in.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.out.println("here1");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("here2");
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("here3");
		}

	}

	@Override
	public boolean checkIfEventHasEnded(Event event) {
		return (event.getStatus().equals("FT"))?true:false;
	}

}
