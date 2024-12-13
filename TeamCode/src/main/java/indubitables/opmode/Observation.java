package indubitables.opmode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import indubitables.config.subsystem.IntakeSubsystem;
import indubitables.pedroPathing.follower.Follower;
import indubitables.config.runmodes.Auto;
import indubitables.pedroPathing.util.Timer;

@Autonomous(name="Observation", group=".")
public class Observation extends OpMode {
    public int pathState;
    public Auto auto;

    public Timer pathTimer = new Timer();

    @Override
    public void init() {
        auto = new Auto(hardwareMap, telemetry, new Follower(hardwareMap), true, false);
    }

    @Override
    public void start() {
        auto.start();
        setPathState(0);
    }

    @Override
    public void loop() {
        telemetry.addData("State: ", pathState);
        telemetry.addData("Path Timer: ", pathTimer.getElapsedTimeSeconds());
        auto.update();
        pathUpdate();
    }

    public void pathUpdate() {
        switch (pathState) {
            case 0: //Runs to the position of the preload and holds it's point at 0.5 power
                auto.liftPIDF = false;
                auto.extend.toZero();
                auto.startChamber();
                setPathState(999);
                break;
            case 999:
                if(pathTimer.getElapsedTimeSeconds() > 0.5) {
                    auto.follower.setMaxPower(0.85);
                    auto.follower.followPath(auto.preload, false);
                    setPathState(1);
                }
                break;
            case 1: //Once Chamber State Machine finishes, begins Pathchain to push elements to the submersible
                if(auto.actionNotBusy()) {
                    auto.outtake.open();
                    auto.follower.setMaxPower(0.9);
                    auto.follower.followPath(auto.preload1, true);
                    setPathState(989);
                }
                break;
            case 989:
                if(pathTimer.getElapsedTimeSeconds() > 0.5) {
                    auto.outtake.specimenGrab();
                    auto.extend.toFull();
                    auto.intake.hover();
                    auto.intake.rotateDegrees(-45);
                    setPathState(1000);
                }
                break;
            case 1000:
                if(pathTimer.getElapsedTimeSeconds() > 1.75) {
                    auto.intake.ground();
                    setPathState(2000);
                }
                break;
            case 2000:
                if(pathTimer.getElapsedTimeSeconds() > 0.25) {
                    auto.intake.close();
                    setPathState(2001);
                }
                break;
            case 2001:
                if(pathTimer.getElapsedTimeSeconds() > 0.25) {
                    auto.outtake.specimenScore();
                    auto.intake.hover();
                    auto.follower.followPath(auto.dropoff1, true);
                    setPathState(1001);
                }
                break;
            case 1001:
                if(pathTimer.getElapsedTimeSeconds() > 1) {
                    auto.intake.open();
                    auto.intake.rotateDegrees(-60);
                    auto.follower.followPath(auto.preload2, true);
                    setPathState(2002);
                }
                break;
            case 2002:
                if(pathTimer.getElapsedTimeSeconds() > 1.5) {
                    auto.intake.ground();
                    setPathState(2003);
                }
                break;
            case 2003:
                if(pathTimer.getElapsedTimeSeconds() > 0.35) {
                    auto.intake.close();
                    setPathState(1002);
                }
                break;
            case 1002:
                if(pathTimer.getElapsedTimeSeconds() > 0.25) {
                    auto.intake.hover();
                    auto.follower.followPath(auto.dropoff2, true);
                    setPathState(1003);
                }
                break;
            case 1003:
                if(!auto.follower.isBusy()) {
                    auto.intake.open();
                    auto.intake.rotateDegrees(-60);
                    auto.follower.followPath(auto.preload3, true);
                    setPathState(2004);
                }
                break;
            case 2004:
                if(pathTimer.getElapsedTimeSeconds() > 1.5) {
                    auto.intake.ground();
                    setPathState(2005);
                }
                break;
            case 2005:
                if(pathTimer.getElapsedTimeSeconds() > 0.25) {
                    auto.intake.close();
                    setPathState(1004);
                }
                break;
            case 1004:
                if(pathTimer.getElapsedTimeSeconds() > 0.25) {
                    auto.intake.hover();
                    auto.follower.followPath(auto.dropoff3, true);
                    setPathState(1005);
                }
                break;
            case 1005:
                if(!auto.follower.isBusy()) {
                    auto.intake.specimen();
                    auto.intake.open();
                    auto.extend.toZero();
                    auto.follower.followPath(auto.setup, true);
                    setPathState(3);
                }
                break;
            case 3:
                if(pathTimer.getElapsedTimeSeconds() > 0.5) {
                    auto.startSpecimen();
                    setPathState(2);
                }
            case 2: //Once the Pathchain finishes, begins the Specimen State Machine
                if() {
                    auto.follower.setMaxPower(0.9);
                    auto.follower.followPath(auto.grab1, false);
                    setPathState(4);
                }
                break;
            case 4: //Closes the claw when the follower reaches the grab1 position
                if(pathTimer.getElapsedTimeSeconds() > 0.35) {
                    auto.outtake.close();
                    setPathState(5);
                }
                break;
            case 5: //Sets the arm to a neutral position and puts lifts to zero;
                if(pathTimer.getElapsedTimeSeconds() > 0.25) {
                    auto.follower.setMaxPower(0.8);
                    auto.follower.followPath(auto.specimen1, true);
                    setPathState(6);
                }
                break;
            case 6: //Waits until follower reaches it's position then begins the Chamber State Machine
                if(pathTimer.getElapsedTimeSeconds() > 0) {
                    auto.startChamber();
                    setPathState(7);
                }
                break;
            case 7: //Starts the Specimen State Machine
                if(auto.actionNotBusy()) {
                    auto.follower.setMaxPower(0.9);
                    auto.follower.followPath(auto.grab2, true);
                    setPathState(8);
                }
                break;
            case 8: //Begins the path for grab 2 & closes the claw once it reaches position and passes 0.75 seconds
                if(pathTimer.getElapsedTimeSeconds() > 1) {
                    auto.startSpecimen();
                    setPathState(9);
                }
                break;
            case 9:
                if(pathTimer.getElapsedTimeSeconds() > 1) {
                    auto.outtake.close();
                    setPathState(10);
                }
                break;
            case 10: //Waits 0.25 seconds and puts robot in neutral position
                if(pathTimer.getElapsedTimeSeconds() > 0.25) {
                    //auto.init();
                    setPathState(11);
                }
                break;
            case 11: //Drives to chamber once action finishes
                 auto.follower.setMaxPower(0.8);
                 auto.follower.followPath(auto.specimen2, true);
                 setPathState(12);
                break;
            case 12: //Starts the Chamber State Machine
                if(pathTimer.getElapsedTimeSeconds() > 0) {
                    auto.startChamber();
                    setPathState(13);
                }
                break;
            case 13: //Starts the Specimen State Machine
                if(auto.actionNotBusy()) {
                    auto.follower.setMaxPower(0.9);
                    auto.follower.followPath(auto.grab3, true);
                    setPathState(14);
                }
                break;
            case 14: //Begins the path for grab 2 & closes the claw once it reaches position and passes 0.75 seconds
                if(pathTimer.getElapsedTimeSeconds() > 1) {
                    auto.startSpecimen();
                    setPathState(15);
                }
                break;
            case 15:
                if(pathTimer.getElapsedTimeSeconds() > 1) {
                   auto.outtake.close();
                    setPathState(16);
                }
                break;
            case 16: //Waits 0.25 seconds and puts robot in neutral position
                if(pathTimer.getElapsedTimeSeconds() > 0.25) {
                   //auto.init();
                    setPathState(17);
                }
                break;
            case 17: //Drives to chamber once action finishes
                auto.follower.setMaxPower(0.9);
                auto.follower.followPath(auto.specimen3, true);
                setPathState(18);
                break;
            case 18: //Starts the Chamber State Machine
                if(pathTimer.getElapsedTimeSeconds() > 0) {
                    auto.startChamber();
                    setPathState(19);
                }
                break;
            case 19: //Starts the Specimen State Machine
                if(auto.actionNotBusy()) {
                    auto.follower.setMaxPower(0.9);
                    auto.follower.followPath(auto.grab4, true);
                    setPathState(20);
                }
                break;
            case 20: //Begins the path for grab 2 & closes the claw once it reaches position and passes 0.75 seconds
                if(pathTimer.getElapsedTimeSeconds() > 1) {
                    auto.startSpecimen();
                    setPathState(21);
                }
                break;
            case 21:
                if(pathTimer.getElapsedTimeSeconds() > 1) {
                    auto.outtake.close();
                    setPathState(22);
                }
                break;
            case 22: //Waits 0.25 seconds and puts robot in neutral position
                if(pathTimer.getElapsedTimeSeconds() > 0.25) {
                    //auto.intake.init();
                    setPathState(23);
                }
                break;
            case 23: //Drives to chamber once action finishes
                auto.follower.setMaxPower(0.9);
                auto.follower.followPath(auto.specimen4, true);
                setPathState(24);
                break;
            case 24: //Starts the Chamber State Machine
                if(pathTimer.getElapsedTimeSeconds() > 0) {
                    auto.startChamber();
                    setPathState(25);
                }
                break;
            case 25: //Park and End the autonomous
                if(auto.actionNotBusy()) {
                    auto.follower.setMaxPower(1);
                    auto.follower.followPath(auto.park, true);
                    auto.outtake.open();
                    auto.extend.toFull();
                    auto.intake.hover();
                    setPathState(26);
                }
                break;
            case 26:
                if(pathTimer.getElapsedTimeSeconds() > 0.5) {
                   // auto.intake.pivotGround();
                   // auto.intake.spinIn();
                    setPathState(-1);
                }
                break;
        }
    }

    public void setPathState(int x) {
        pathState = x;
        pathTimer.resetTimer();
    }
}
