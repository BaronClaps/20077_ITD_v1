package indubitables.config.runmodes;

import static indubitables.config.util.FieldConstants.*;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import indubitables.pedroPathing.pathGeneration.BezierCurve;
import indubitables.config.subsystem.ArmSubsystem;
import indubitables.config.subsystem.ClawSubsystem;
import indubitables.config.subsystem.ExtendSubsystem;
import indubitables.config.subsystem.IntakeSubsystem;
import indubitables.config.subsystem.LiftSubsystem;
import indubitables.pedroPathing.follower.Follower;
import indubitables.pedroPathing.localization.Pose;
import indubitables.pedroPathing.pathGeneration.BezierLine;
import indubitables.pedroPathing.pathGeneration.Path;
import indubitables.pedroPathing.pathGeneration.Point;
import indubitables.config.util.action.Action;
import indubitables.config.util.action.ParallelAction;
import indubitables.config.util.action.RunAction;
import indubitables.config.util.action.SequentialAction;
import indubitables.config.util.action.SleepAction;

public class Auto {

    private RobotStart startLocation;

    public ClawSubsystem claw;
    public ClawSubsystem.ClawGrabState clawGrabState;
    public ClawSubsystem.ClawPivotState clawPivotState;
    public LiftSubsystem lift;
    public ExtendSubsystem extend;
    public IntakeSubsystem intake;
    public IntakeSubsystem.IntakeSpinState intakeSpinState;
    public IntakeSubsystem.IntakePivotState intakePivotState;
    public ArmSubsystem arm;
    public ArmSubsystem.ArmState armState;


    public Follower follower;
    public Telemetry telemetry;

    public RunAction transfer;
    public Path preload, element1, score1, element2, score2, element3, score3, park;
    private Pose startPose, preloadPose, element1Pose, element1ControlPose, element2Pose, element2ControlPose, element3Pose, element3ControlPose, elementScorePose, parkControlPose, parkPose;

    public Auto(HardwareMap hardwareMap, Telemetry telemetry, Follower follower, boolean isBlue, boolean isBucket) {
        claw = new ClawSubsystem(hardwareMap, clawGrabState, clawPivotState);
        lift = new LiftSubsystem(hardwareMap, telemetry);
        extend = new ExtendSubsystem(hardwareMap, telemetry);
        intake = new IntakeSubsystem(hardwareMap, intakeSpinState, intakePivotState);
        arm = new ArmSubsystem(hardwareMap, armState);

        this.follower = follower;
        this.telemetry = telemetry;

        startLocation = isBlue ? (isBucket ? RobotStart.BLUE_BUCKET : RobotStart.BLUE_OBSERVATION) : (isBucket ? RobotStart.RED_BUCKET : RobotStart.RED_OBSERVATION);
        
        createPoses();
        buildPaths();

        //transfer = new RunAction(this::transfer);

        init();
    }

    public void init() {
        follower.setStartingPose(startPose);
        claw.init();
        //lift.init();
        //extend.init();
        // intake.init();
        arm.init();
    }

    public void init_loop() {}

    public void start() {
        claw.start();
       // lift.start();
        extend.start();
        //intake.start();
        arm.start();
    }

    public void update() {
        follower.update();
        //lift.updatePIDF();
        //extend.updatePIDF();
    }

    public void createPoses() {
        switch (startLocation) {
            case BLUE_BUCKET:
                startPose = blueBucketStartPose;
                preloadPose = blueBucketPreloadPose;
                element1ControlPose = blueBucketLeftSampleControlPose;
                element1Pose = blueBucketLeftSamplePose;
                element2ControlPose = blueBucketMidSampleControlPose;
                element2Pose = blueBucketMidSamplePose;
                element3ControlPose = blueBucketRightSampleControlPose;
                element3Pose = blueBucketRightSamplePose;
                elementScorePose = blueBucketScorePose;
                parkControlPose = blueBucketParkControlPose;
                parkPose = blueBucketParkPose;
                break;

            case BLUE_OBSERVATION:
                startPose = blueObservationStartPose;
                //parkPose = blueObservationPark;
                break;

            case RED_BUCKET:
                startPose = redBucketStartPose;
                //parkPose = redBucketPark;
                break;

            case RED_OBSERVATION:
                startPose = redObservationStartPose;
                //parkPose = redObservationPark;
                break;
        }

    }

    public void buildPaths() {
        follower.setStartingPose(startPose);

        preload = new Path(new BezierLine(new Point(startPose), new Point(preloadPose)));
        preload.setLinearHeadingInterpolation(startPose.getHeading(), preloadPose.getHeading());

        element1 = new Path(new BezierCurve(new Point(preloadPose), new Point(element1ControlPose), new Point(element1Pose)));
        element1.setLinearHeadingInterpolation(preloadPose.getHeading(), element1Pose.getHeading());

        score1 = new Path(new BezierLine(new Point(element1Pose), new Point(elementScorePose)));
        score1.setLinearHeadingInterpolation(element1Pose.getHeading(), elementScorePose.getHeading());

        element2 = new Path(new BezierCurve(new Point(element1Pose), new Point(element2ControlPose), new Point(element2Pose)));
        element2.setLinearHeadingInterpolation(element1Pose.getHeading(), element2Pose.getHeading());

        score2 = new Path(new BezierLine(new Point(element2Pose), new Point(elementScorePose)));
        score2.setLinearHeadingInterpolation(element2Pose.getHeading(), elementScorePose.getHeading());

        element3 = new Path(new BezierCurve(new Point(element2Pose), new Point(element3ControlPose), new Point(element3Pose)));
        element3.setLinearHeadingInterpolation(element2Pose.getHeading(), element3Pose.getHeading());

        score3 = new Path(new BezierLine(new Point(element3Pose), new Point(elementScorePose)));
        score3.setLinearHeadingInterpolation(element3Pose.getHeading(), elementScorePose.getHeading());

        park = new Path(new BezierCurve(new Point(elementScorePose), new Point(parkControlPose), new Point(parkPose)));
        park.setLinearHeadingInterpolation(elementScorePose.getHeading(), parkPose.getHeading());
    }

    private Action transfer() {
        return new SequentialAction(
                intake.spinStop,
                new ParallelAction(
                        intake.pivotTransfer,
                        arm.toTransfer),
                intake.spinOut,
                new SleepAction(1),
                intake.spinIn,
                intake.spinStop
        );
    }
}