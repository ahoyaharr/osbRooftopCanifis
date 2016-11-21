import org.osbot.P;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.GroundItem;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.RS2Object;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.api.ui.Spells;
import org.osbot.rs07.api.ui.Tab;
import org.osbot.rs07.api.util.ExperienceTracker;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@ScriptManifest(name = "aleksandrRooftopCanifis", author = "Solzhenitsyn", version = 0.0, info = "OSBot Template", logo = "")
public class aleksandrRooftopCanifis extends Script {

    private Timer timer;
    private Cursor m;
    private aleksandrMethods a;
    private String status = "Initializing script...";

    private ExperienceTracker xp;

    public void onStart() {
        a = new aleksandrMethods(this);
        m = new Cursor(this);
        timer = new Timer(System.currentTimeMillis());
        xp = getExperienceTracker();
        xp.startAll();
    }

    private String parse(long millis) {
        String time = "n/a";
        if (millis > 0) {
            int seconds = (int) (millis / 1000) % 60;
            int minutes = (int) ((millis / (1000 * 60)) % 60);
            int hours = (int) ((millis / (1000 * 60 * 60)) % 24);
            time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return time;
    }

    public void onPaint(Graphics2D g) { // NOT DONE
        m.draw(g);
        g.setColor(new Color(255, 255, 255, 255));
        g.setFont(new Font("Tahoma", Font.PLAIN, 14));
        g.drawString("Currently " + status, 12, 48);
        g.drawString("Timer: " + timer.parse(timer.getElapsed()), 12, 62);
        g.drawString("Magic XP: " + xp.getGainedXP(Skill.MAGIC) +
                " (" + xp.getGainedXPPerHour(Skill.MAGIC) + " - " + parse(xp.getTimeToLevel(Skill.MAGIC)) + ")", 12, 74);
        g.drawString("Agility XP: " + xp.getGainedXP(Skill.AGILITY) +
                " (" + xp.getGainedXPPerHour(Skill.AGILITY) + " - " + parse(xp.getTimeToLevel(Skill.AGILITY)) + ")", 12, 86);

        g.setFont(new Font("Tahoma", Font.BOLD, 14));
        g.drawString("aleksandrRooftopCanifis", 12, 34);

        if (nextObstacle != null && nextObstacle.exists() && nextObstacle.isVisible()) {
            g.drawPolygon(nextObstacle.getPosition().getPolygon(getBot()));
        }
        if (nextMark != null && nextMark.exists() && nextMark.isVisible()) {
            g.drawPolygon(nextMark.getPosition().getPolygon(getBot()));
        }
    }


    private List<String> mark = Arrays.asList("Mark of grace");

    class obstacle {
        public Position pos;
        public Integer id;
        public String name;
        public String action;
        public cameraLoc cam;

        public obstacle(Position pos, Integer id, String action, String name, cameraLoc cam) {
            this.pos = pos;
            this.id = id;
            this.action = action;
            this.name = name;
            this.cam = cam;
        }
    }

    class cameraLoc {
        public int pitch;
        public int yaw;

        public cameraLoc(int pitch, int yaw) {
            this.pitch = pitch;
            this.yaw = yaw;
        }
    }

    List<obstacle> obstacles = Arrays.asList(
            new obstacle(new Position(3505, 3489, 0), 10819, "Climb", "Tall tree", new cameraLoc(60, 10)),
            new obstacle(new Position(3505, 3498, 2), 10820, "Jump", "Gap", new cameraLoc(60, 90)),
            new obstacle(new Position(3496, 3504, 2), 10821, "Jump", "Gap", new cameraLoc(60, 90)),
            new obstacle(new Position(3485, 3499, 2), 10828, "Jump", "Gap", new cameraLoc(60, 90)),
            new obstacle(new Position(3478, 3491, 3), 10822, "Jump", "Gap", new cameraLoc(60, 180)),
            new obstacle(new Position(3480, 3483, 2), 10831, "Vault", "Pole-vault", new cameraLoc(67, 270)),
            new obstacle(new Position(3503, 3476, 3), 10823, "Jump", "Gap", new cameraLoc(25, 270)),
            new obstacle(new Position(3510, 3483, 2), 10832, "Jump", "Gap", new cameraLoc(60, 10))
    );

    private obstacle getObstacle() {
        for (obstacle o : obstacles) {
            RS2Object next = a.getRS2Object(o.pos, o.id);
            if (next != null && getMap().canReach(next)) {
                log("Current obstacle: " + obstacles.indexOf(o) + 1);
                return o;
            }
        }
        return obstacles.get(0);
    }

    private boolean alch() {
        Spells.NormalSpells alch_type = (getSkills().getDynamic(Skill.MAGIC) >= 55)
                ? Spells.NormalSpells.HIGH_LEVEL_ALCHEMY : Spells.NormalSpells.LOW_LEVEL_ALCHEMY;
        if (a.hasNotedItem()) {
            if (a.castSpell(alch_type)) {
                if (getMagic().getSelectedSpellName().contains("Alchemy")) {
                    new cSleep(() -> getTabs().getOpen().equals(Tab.INVENTORY), 500).sleep();
                    a.clickPoint(a.getPointOfNextNotedItem());
                    return true;
                } else {
                    alch();
                }
            }
        }
        return false;
    }

    RS2Object nextObstacle = null;
    GroundItem nextMark = null;

    private void handleObstacle(obstacle o) {
        Integer curAgi = xp.getGainedXP(Skill.AGILITY);
        Integer curMag = xp.getGainedXP(Skill.MAGIC);
        Integer curHp = getSkills().getDynamic(Skill.HITPOINTS);
        Point nextPos = null;
        nextMark = a.getLootableItem(mark, 10);

        cameraLoc cam = null;
        Integer index = obstacles.indexOf(o);
        if (index == -1 || index + 1 == obstacles.size()) {
            cam = obstacles.get(0).cam;
        } else {
            cam = obstacles.get(index + 1).cam;
        }

        nextObstacle = a.getRS2Object(o.pos, o.id);
        if (nextObstacle != null) {
            if (myPlayer().isHitBarVisible()) {
                try {
                    log("recovering from failed obstacle...");
                    status = "recovering from failed obstacle...";
                    sleep(random(1500, 2000));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (myPosition().getZ() == 0 && !nextObstacle.isVisible()) { // walk to start location
                a.walkToPosition(new Position(3505, 3488, 0));
            } else if (nextMark != null) {
                log("looting mark...");
                status = "looting mark...";
                nextPos = a.getPointOfGroundItem(nextMark);
                a.rightClickByTooltip(nextPos, "Take", nextMark);
            } else {
                a.walkToPosition(nextObstacle.getPosition());
            }

            log("casting alchemy...");
            status = "casting alchemy...";
            alch();

            if (nextMark != null) {
                log("looting mark...");
                status = "looting mark...";
                nextPos = a.getPointOfGroundItem(nextMark);
                a.rightClickByTooltip(nextPos, "Take", nextMark);
                new cSleep(() -> a.getLootableItem(mark, 10) == null, 5000).sleep();
            } else if (nextObstacle.isVisible()) {
                a.rightClickByTooltip(nextPos, o.action, o.name);
            }

            nextPos = a.getPointOfRS2Object(nextObstacle);
            if (xp.getGainedXP(Skill.MAGIC) > curMag) {
                status = "casting alchemy...";
                log("casting alchemy...");
                alch();
            }
            log(o.action.toLowerCase() + "ing " + o.name.toLowerCase() + "...");
            if (myPosition().getZ() == 0) {
                nextObstacle.interact(o.action);
            } else {
                if (nextObstacle.isVisible()) {
                    a.rightClickByTooltip(nextPos, o.action, o.name);
                }
            }
            if (cam != null) {
                status = "setting up camera...";
                log("setting up camera...");
                log("Next camera position: pitch = " + cam.pitch + ", yaw = " + cam.yaw);
                a.setCameraPosition(cam.pitch, cam.yaw, 0.1);
            }
            a.deselect();
            new cSleep(() -> curAgi < xp.getGainedXP(Skill.AGILITY) || curHp < getSkills().getDynamic(Skill.HITPOINTS), 3750).sleep();
        }
    }

    public int onLoop() throws InterruptedException {
        handleObstacle(getObstacle());
        return 100;
    }
}

