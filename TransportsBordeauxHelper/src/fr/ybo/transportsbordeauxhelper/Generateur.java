/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     ybonnel - initial API and implementation
 */
package fr.ybo.transportsbordeauxhelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ybo.moteurcsv.MoteurCsv;
import fr.ybo.transportsbordeauxhelper.exception.TbcException;
import fr.ybo.transportsbordeauxhelper.gtfs.GestionnaireGtfs;
import fr.ybo.transportsbordeauxhelper.gtfs.modele.Calendar;
import fr.ybo.transportsbordeauxhelper.gtfs.modele.CalendarDates;
import fr.ybo.transportsbordeauxhelper.gtfs.modele.Route;
import fr.ybo.transportsbordeauxhelper.gtfs.modele.Stop;
import fr.ybo.transportsbordeauxhelper.gtfs.modele.StopTime;
import fr.ybo.transportsbordeauxhelper.gtfs.modele.Trip;
import fr.ybo.transportsbordeauxhelper.modele.Arret;
import fr.ybo.transportsbordeauxhelper.modele.ArretRoute;
import fr.ybo.transportsbordeauxhelper.modele.Calendrier;
import fr.ybo.transportsbordeauxhelper.modele.CalendrierException;
import fr.ybo.transportsbordeauxhelper.modele.Direction;
import fr.ybo.transportsbordeauxhelper.modele.Horaire;
import fr.ybo.transportsbordeauxhelper.modele.Ligne;
import fr.ybo.transportsbordeauxhelper.modele.Trajet;

/**
 * Générateur.
 * @author ybonnel
 *
 */
public class Generateur {

	private List<Ligne> lignes = new ArrayList<Ligne>();
	private List<Calendrier> calendriers = new ArrayList<Calendrier>();
	private List<CalendrierException> calendriersException = new ArrayList<CalendrierException>();
	private Map<String, List<Trajet>> trajets = new HashMap<String, List<Trajet>>();
	private Map<Integer, Direction> directions = new HashMap<Integer, Direction>();
	private Map<String, Integer> mapDirectionIds = null;
	private List<Horaire> horaires = new ArrayList<Horaire>();
	private Map<String, Arret> arrets = new HashMap<String, Arret>();
	private List<ArretRoute> arretsRoutes = new ArrayList<ArretRoute>();
	private Map<String, List<Horaire>> horairesByLigneId = new HashMap<String, List<Horaire>>();

	private static final List<Class<?>> LIST_CLASSES = new ArrayList<Class<?>>();

	static {
		LIST_CLASSES.add(Arret.class);
		LIST_CLASSES.add(ArretRoute.class);
		LIST_CLASSES.add(Calendrier.class);
		LIST_CLASSES.add(CalendrierException.class);
		LIST_CLASSES.add(Direction.class);
		LIST_CLASSES.add(Horaire.class);
		LIST_CLASSES.add(Ligne.class);
		LIST_CLASSES.add(Trajet.class);
	}

	public void rechercherPointsInterets() {
		int max = 0;
		Arret arretLong = null;
		for (Arret arret : arrets.values()) {
			if (arret.nom.length() > max) {
				max = arret.nom.length();
				arretLong = arret;
			}
		}
		System.out.println("Arret avec le nom le plus long : " + arretLong.nom + " qui existe sur les lignes :");
		for (ArretRoute arretRoute : arretsRoutes) {
			if (arretRoute.arretId.equals(arretLong.id)) {
				System.out.println("\t" + arretRoute.ligneId);
			}
		}


		max = 0;
		Direction directionLongue = null;
		for (Direction direction : directions.values()) {
			if (direction.direction.length() > max) {
				max = direction.direction.length();
				directionLongue = direction;
			}
		}
		for (ArretRoute arretRoute : arretsRoutes) {
			if (arretRoute.directionId == directionLongue.id) {
				System.out.println("Direction avec le nom le plus long : " + directionLongue.direction
						+ " pour la ligne " + arretRoute.ligneId);
				break;
			}
		}

	}

	public void genererFichiers(File repertoire) {
		if (repertoire.exists()) {
			for (File file : repertoire.listFiles()) {
				if (!file.delete()) {
					System.err.println("Le fichier " + file.getName() + "n'a pas pu être effacé");
				}
			}
		} else {
			if (!repertoire.mkdirs()) {
				System.err.println("Le répertoire " + repertoire.getName() + "n'a pas pu être créé");
			}
		}
		MoteurCsv moteurCsv = new MoteurCsv(LIST_CLASSES);
		System.out.println("Génération du fichier arrets.txt");
		moteurCsv.writeFile(new File(repertoire, "arrets.txt"), arrets.values(), Arret.class);
		System.out.println("Génération du fichier arrets_routes.txt");
		moteurCsv.writeFile(new File(repertoire, "arrets_routes.txt"), arretsRoutes, ArretRoute.class);
		System.out.println("Génération du fichier calendriers.txt");
		moteurCsv.writeFile(new File(repertoire, "calendriers.txt"), calendriers, Calendrier.class);
		System.out.println("Génération du fichier calendriers_exceptions.txt");
		moteurCsv.writeFile(new File(repertoire, "calendriers_exceptions.txt"), calendriersException,
				CalendrierException.class);
		System.out.println("Génération du fichier directions.txt");
		moteurCsv.writeFile(new File(repertoire, "directions.txt"), directions.values(), Direction.class);
		System.out.println("Génération du fichier horaires.txt");
		moteurCsv.writeFile(new File(repertoire, "horaires.txt"), horaires, Horaire.class);
		System.out.println("Génération du fichier lignes.txt");
		moteurCsv.writeFile(new File(repertoire, "lignes.txt"), lignes, Ligne.class);
		System.out.println("Génération du fichier trajets.txt");
		List<Trajet> trajets = new ArrayList<Trajet>();
		for (List<Trajet> trajetsToAdd : this.trajets.values()) {
			trajets.addAll(trajetsToAdd);
		}
		moteurCsv.writeFile(new File(repertoire, "trajets.txt"), trajets, Trajet.class);
		for (Ligne ligne : lignes) {
			moteurCsv.writeFile(new File(repertoire, "horaires_" + ligne.id + ".txt"), horairesByLigneId.get(ligne.id),
					Horaire.class);
			System.out.println("Nombre d'horaire pour la ligne " + ligne.nomCourt + " : "
					+ horairesByLigneId.get(ligne.id).size());
		}
		genereZips(repertoire);
	}

	private void genereZips(File repertoire) {

		System.out.println("Création du zip principal");
		try {

			System.out.println("Création du fichier last_update.txt");
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter(new File(repertoire, "last_update.txt")));

			bufWriter.write(new SimpleDateFormat("yyyyMMdd").format(new Date()));

			bufWriter.close();
		} catch (Exception exception) {
			throw new TbcException(exception);
		}
	}

	public void remplirArretRoutes() {
		ArretRoute arretRoute;
		Map<Integer, List<Horaire>> mapHorairesByTrajetId = new HashMap<Integer, List<Horaire>>();
		for (Horaire horaire : horaires) {
			if (!mapHorairesByTrajetId.containsKey(horaire.trajetId)) {
				mapHorairesByTrajetId.put(horaire.trajetId, new ArrayList<Horaire>());
			}
			mapHorairesByTrajetId.get(horaire.trajetId).add(horaire);
		}
		// Trip des horaires.
		for (List<Horaire> horairesATrier : mapHorairesByTrajetId.values()) {
			Collections.sort(horairesATrier, new Comparator<Horaire>() {
				public int compare(Horaire o1, Horaire o2) {
					return (o1.stopSequence < o2.stopSequence ? -1 : (o1.stopSequence == o2.stopSequence ? 0 : 1));
				}
			});
		}
		for (Ligne ligne : lignes) {
			horairesByLigneId.put(ligne.id, new ArrayList<Horaire>());
			Map<String, Integer> countByChaine = new HashMap<String, Integer>();
			Map<String, List<Trajet>> mapTrajetChaine = new HashMap<String, List<Trajet>>();
			Map<String, Arret> arretOfLigne = new HashMap<String, Arret>();
			// Parcours des trajets.
			for (Trajet trajet : trajets.get(ligne.id)) {
				StringBuilder chaineBuilder = new StringBuilder();
				Horaire terminus = null;
				for (Horaire horaire : mapHorairesByTrajetId.get(trajet.id)) {
					horairesByLigneId.get(ligne.id).add(horaire);
					if (!arretOfLigne.containsKey(horaire.arretId)) {
						if (!arrets.containsKey(horaire.arretId)) {
							System.err.println("Arret inconnu : " + horaire.arretId);
						}
						arretOfLigne.put(horaire.arretId, arrets.get(horaire.arretId));
					}
					chaineBuilder.append(horaire.arretId);
					chaineBuilder.append(',');
					terminus = horaire;
				}
				terminus.terminus = true;
				if (!countByChaine.containsKey(chaineBuilder.toString())) {
					countByChaine.put(chaineBuilder.toString(), 0);
					mapTrajetChaine.put(chaineBuilder.toString(), new ArrayList<Trajet>());
				}
				countByChaine.put(chaineBuilder.toString(), countByChaine.get(chaineBuilder.toString()) + 1);
				mapTrajetChaine.get(chaineBuilder.toString()).add(trajet);
			}
			// parcours des arrêts
			for (Arret arret : arretOfLigne.values()) {
				arretRoute = new ArretRoute();
				arretRoute.arretId = arret.id;
				arretRoute.ligneId = ligne.id;
				// Recherche du trajet adéquat.
				int max = 0;
				String chaine = null;
				for (Map.Entry<String, Integer> entryChaineCount : countByChaine.entrySet()) {
					if (entryChaineCount.getValue() > max
							&& (entryChaineCount.getKey().startsWith(arret.id + ",") || (!entryChaineCount.getKey()
									.endsWith("," + arret.id + ",") && entryChaineCount.getKey().contains(
									"," + arret.id + ",")))) {
						// Chemin trouvé
						max = entryChaineCount.getValue();
						chaine = entryChaineCount.getKey();
					}
				}
				if (chaine == null) {
					// Seulement terminus pour cette ligne, pas à gérer
					continue;
				}
				String[] champs = chaine.split(",");
				int sequence = 1;
				for (String champ : champs) {
					if (champ.equals(arret.id)) {
						break;
					}
					sequence++;
				}
				arretRoute.sequence = sequence;
				Map<Integer, Integer> countDirectionIds = new HashMap<Integer, Integer>();
				for (Trajet trajet : mapTrajetChaine.get(chaine)) {
					if (!countDirectionIds.containsKey(trajet.directionId)) {
						countDirectionIds.put(trajet.directionId, 0);
					}
					countDirectionIds.put(trajet.directionId, countDirectionIds.get(trajet.directionId) + 1);
				}
				int directionId = -1;
				max = 0;
				for (Map.Entry<Integer, Integer> entryDirectionIdCount : countDirectionIds.entrySet()) {
					if (entryDirectionIdCount.getValue() > max) {
						directionId = entryDirectionIdCount.getKey();
						max = entryDirectionIdCount.getValue();
					}
				}
				if (countDirectionIds.size() > 1) {
					System.err.println("Plusieurs directions trouvée pour une seule chaine :");
					System.err.println("\t" + chaine);
					for (int dirId : countDirectionIds.keySet()) {

						System.err.println(directions.get(dirId).direction);
					}
					System.err.println("Direction choisi (la plus utilisée) :");
					System.err.println("\t" + directions.get(directionId).direction);
				}
				if (directionId == -1) {
					System.err.println("Pas de direction trouvée!!!!!");
				}
				arretRoute.directionId = directionId;
				arretsRoutes.add(arretRoute);
			}
		}
	}

	public void afficheCompteurs() {
		System.out.println("Nomre de lignes : " + lignes.size());
		System.out.println("Nomre de calendrier : " + calendriers.size());
		System.out.println("Nomre de trajets : " + trajets.size());
		System.out.println("Nomre de directions : " + directions.size());
		System.out.println("Nomre d'horaires : " + horaires.size());
		System.out.println("Nomre d'arrêts : " + arrets.size());
		System.out.println("Nomre d'arretRoutes : " + arretsRoutes.size());
	}

	public void remplirArrets() {
		Arret arret;
		for (Stop stop : GestionnaireGtfs.getInstance().getStops().values()) {
			arret = new Arret();
			arret.id = stop.id;
			arret.nom = stop.nom;
			arret.latitude = stop.latitude;
			arret.longitude = stop.longitude;
			arrets.put(arret.id, arret);
		}
	}

	public void remplirHoraires() {
		Horaire horaire;
		for (StopTime stopTime : GestionnaireGtfs.getInstance().getStopTimes().values()) {
			horaire = new Horaire();
			horaire.arretId = stopTime.stopId;
			horaire.trajetId = Integer.parseInt(stopTime.tripId);
			horaire.heureDepart = stopTime.heureDepart;
			horaire.stopSequence = stopTime.stopSequence;
			horaire.terminus = false;
			horaires.add(horaire);
		}
	}

	public void remplirTrajets() {
		remplirDirections();
		Trajet trajet;
		for (Trip trip : GestionnaireGtfs.getInstance().getTrips().values()) {
			trajet = new Trajet();
			trajet.id = Integer.parseInt(trip.id);
			trajet.calendrierId = Integer.parseInt(trip.serviceId);
			trajet.ligneId = trip.routeId;
			trajet.directionId = mapDirectionIds.get(trip.headSign);
			if (!trajets.containsKey(trajet.ligneId)) {
				trajets.put(trajet.ligneId, new ArrayList<Trajet>());
			}
			trajets.get(trajet.ligneId).add(trajet);
		}
	}

	public void remplirDirections() {
		if (mapDirectionIds == null) {
			mapDirectionIds = new HashMap<String, Integer>();
			int directionId = 1;
			for (Trip trip : GestionnaireGtfs.getInstance().getTrips().values()) {
				if (!mapDirectionIds.containsKey(trip.headSign)) {
					mapDirectionIds.put(trip.headSign, directionId++);
				}

			}
			Direction direction;
			for (Map.Entry<String, Integer> headSign : mapDirectionIds.entrySet()) {
				direction = new Direction();
				direction.id = headSign.getValue();
				direction.direction = headSign.getKey();
				if (direction.direction.charAt(0) == '"') {
					direction.direction = direction.direction.substring(1);
				}
				if (direction.direction.charAt(direction.direction.length() - 1) == '"') {
					direction.direction = direction.direction.substring(0, direction.direction.length() - 1);
				}
				directions.put(direction.id, direction);
			}
		}
	}

	public void remplirCalendrier() {
		Calendrier calendrier;
		for (Calendar calendar : GestionnaireGtfs.getInstance().getCalendars().values()) {
			calendrier = new Calendrier();
			calendrier.id = Integer.parseInt(calendar.id);
			calendrier.lundi = calendar.lundi;
			calendrier.mardi = calendar.mardi;
			calendrier.mercredi = calendar.mercredi;
			calendrier.jeudi = calendar.jeudi;
			calendrier.vendredi = calendar.vendredi;
			calendrier.samedi = calendar.samedi;
			calendrier.dimanche = calendar.dimanche;
			calendriers.add(calendrier);
		}
	}

	public void remplirCalendrierException() {
		for (CalendarDates calendarDate : GestionnaireGtfs.getInstance().getCalendarsDates()) {
			CalendrierException calendrierException = new CalendrierException();
			calendrierException.calendrierId = Integer.parseInt(calendarDate.serviceId);
			calendrierException.date = calendarDate.date;
			calendrierException.ajout = calendarDate.exceptionType == 1;
			calendriersException.add(calendrierException);
		}
	}
	
	private boolean isTram(Route route) {
		return route.nomCourt.equals("A") || route.nomCourt.equals("B") || route.nomCourt.equals("C");
	}

	public void remplirLignes() {
		Ligne ligne;
		List<Route> routes = new ArrayList<Route>();
		routes.addAll(GestionnaireGtfs.getInstance().getRoutes().values());
		int maxLength = 0;
		// Recherche de la route avec le nom le plus long.
		for (Route route : routes) {
			if (route.nomLong.charAt(0) == '"') {
				route.nomLong = route.nomLong.substring(1);
			}
			if (route.nomLong.charAt(route.nomLong.length() - 1) == '"') {
				route.nomLong = route.nomLong.substring(0, route.nomLong.length() - 1);
			}
			if (route.nomCourt.length() > maxLength) {
				maxLength = route.nomCourt.length();
			}
		}
		// Tri.
		Collections.sort(routes, new Comparator<Route>() {
			public int compare(Route o1, Route o2) {
				if (isTram(o1) && !isTram(o2)) {
					return -1;
				}
				if (!isTram(o1) && isTram(o2)) {
					return 1;
				}
				return o1.nomCourt.compareTo(o2.nomCourt);
			}
		});

		int ordre = 1;
		for (Route route : routes) {
			ligne = new Ligne();
			ligne.id = route.id;
			ligne.nomCourt = route.nomCourt;
			ligne.nomLong = route.nomLong;
			ligne.ordre = ordre++;
			lignes.add(ligne);
		}
	}
}