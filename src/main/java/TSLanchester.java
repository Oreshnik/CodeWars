import model.Vehicle;
import model.VehicleType;

/**
 * Created by Екатерина on 19.11.2017.
 */
public class TSLanchester {

    public static double calcBattlePoints(Group mine, Group enemy) {
        double dpfEM = getDamage(enemy, mine);
        double dpfME = getDamage(mine, enemy);
        double alpha = dpfEM / (mine.hp * 1.0 / mine.getSize());
        double beta = dpfME / (enemy.hp * 1.0 / enemy.getSize());
        double ra = Math.sqrt(alpha / beta);
        int win = 0;
        if (mine.getSize() * 1.0 / enemy.getSize() > ra) {
            win = 1;
        } else if (mine.getSize() * 1.0 / enemy.getSize() < ra) {
            win = -1;
        }

        if (win == 0) {
            return 0;
        }

        if (win > 0) {
            double survive =  Math.sqrt(mine.getSize() * mine.getSize() - enemy.getSize() * enemy.getSize() * alpha / beta);
            return enemy.getSize() - (mine.getSize() - Math.ceil(survive));
        } else {
            double survive =  Math.sqrt(enemy.getSize() * enemy.getSize() - mine.getSize() * mine.getSize() * beta / alpha);
            return - (mine.getSize() - (enemy.getSize() - Math.ceil(survive)));
        }
    }

    private static int getDamage(Group attacking, Group defencing) {
        return Math.max(0, (defencing.isAerail ? attacking.getAerialDamage() : attacking.getGroundDamage())
                - (attacking.isAerail ? defencing.getAerialDefence() : defencing.getGroundDefence()));
    }

    private static Vehicle createVehicle(int id, int damage, int defence) {
        return new Vehicle(id, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, damage, 0, defence, 0, 0, 0, null, false, false, new int[0]);
    }

    public static void main(String[] args) {
        Group mine = new Group(VehicleType.IFV, MyStrategy.Ownership.ALLY);
        for (int i = 0; i < 20; i++) {
            mine.addVehicle(createVehicle(i,90, 60));
        }
        Group enemy = new Group(VehicleType.IFV, MyStrategy.Ownership.ENEMY);
        for (int i = 0; i < 10; i++) {
            enemy.addVehicle(createVehicle(i, 90, 60));
        }
        System.out.println(calcBattlePoints(mine, enemy));

        mine = new Group(VehicleType.IFV, MyStrategy.Ownership.ALLY);
        for (int i = 0; i < 20; i++) {
            mine.addVehicle(createVehicle(i,90, 60));
        }
        enemy = new Group(VehicleType.TANK, MyStrategy.Ownership.ENEMY);
        for (int i = 0; i < 12; i++) {
            enemy.addVehicle(createVehicle(i, 100, 80));
        }
        System.out.println(calcBattlePoints(mine, enemy));
        System.out.println(String.format("%10s", "Hello"));
        System.out.println(String.format("%10s", "Hell"));
    }
}