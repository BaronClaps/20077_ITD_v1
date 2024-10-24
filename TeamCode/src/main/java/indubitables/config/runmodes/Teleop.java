package indubitables.config.runmodes;

import indubitables.config.subsystem.ArmSubsystem;
import indubitables.config.subsystem.ClawSubsystem;
import indubitables.config.subsystem.ExtendSubsystem;
import indubitables.config.subsystem.IntakeSubsystem;
import indubitables.config.subsystem.LiftSubsystem;
import indubitables.pedroPathing.follower.Follower;
import indubitables.pedroPathing.localization.Pose;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import indubitables.config.util.action.Action;
import indubitables.config.util.action.ParallelAction;
import indubitables.config.util.action.RunAction;
import indubitables.config.util.action.SequentialAction;
import indubitables.config.util.action.SleepAction;

public class Teleop {

    private ClawSubsystem claw;
    private ClawSubsystem.ClawGrabState clawGrabState;
    private ClawSubsystem.ClawPivotState clawPivotState;
    private LiftSubsystem lift;
    private ExtendSubsystem extend;
    private IntakeSubsystem intake;
    private IntakeSubsystem.IntakeSpinState intakeSpinState;
    private IntakeSubsystem.IntakePivotState intakePivotState;
    private ArmSubsystem arm;
    private ArmSubsystem.ArmState armState;

    public RunAction stopDrive, startDrive;

    //private Follower follower;
 //   private Pose startPose;

    private Telemetry telemetry;

    private Gamepad gamepad1, gamepad2;
    private Gamepad currentGamepad1 = new Gamepad();
    private Gamepad currentGamepad2 = new Gamepad();
    private Gamepad previousGamepad1 = new Gamepad();
    private Gamepad previousGamepad2 = new Gamepad();

    private DcMotor leftFront, rightFront, leftRear, rightRear;

    public double speed = 0.75;

    private boolean fieldCentric;


    public Teleop(HardwareMap hardwareMap, Telemetry telemetry, Follower follower, Pose startPose,  boolean fieldCentric, Gamepad gamepad1, Gamepad gamepad2) {
        claw = new ClawSubsystem(hardwareMap, clawGrabState, clawPivotState);
        lift = new LiftSubsystem(hardwareMap, telemetry);
        extend = new ExtendSubsystem(hardwareMap, telemetry);
        intake = new IntakeSubsystem(hardwareMap, intakeSpinState, intakePivotState);
        arm = new ArmSubsystem(hardwareMap, armState);


        leftFront = hardwareMap.get(DcMotor.class, "leftFront");
        rightFront = hardwareMap.get(DcMotor.class, "rightFront");
        leftRear = hardwareMap.get(DcMotor.class, "leftRear");
        rightRear = hardwareMap.get(DcMotor.class, "rightRear");

        leftFront.setDirection(DcMotor.Direction.REVERSE);
        leftRear.setDirection(DcMotor.Direction.REVERSE);

        leftFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        leftRear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightRear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

      //  this.follower = follower;
      //  this.startPose = startPose;

        this.fieldCentric = fieldCentric;
        this.telemetry = telemetry;
        this.gamepad1 = gamepad1;
        this.gamepad2 = gamepad2;

        stopDrive = new RunAction(this::stopDrive);
        startDrive = new RunAction(this::startDrive);
    }

    public void init() {
        claw.init();
        extend.init();
        intake.init();
        arm.init();
        initPos();
    }

    public void update() {
        previousGamepad1.copy(currentGamepad1);
        previousGamepad2.copy(currentGamepad2);
        currentGamepad1.copy(gamepad1);
        currentGamepad2.copy(gamepad2);

        if (gamepad1.right_bumper)
            speed = 1;
        else if (gamepad1.left_bumper)
            speed = 0.25;
        else
            speed = 0.75;

        lift.manual(gamepad2.right_trigger - (gamepad2.left_trigger * .75));

        double max;
        double axial = -gamepad1.left_stick_y;  //Pushing stick forward gives negative value
        double lateral = gamepad1.left_stick_x;
        double yaw = gamepad1.right_stick_x;
        double leftFrontPower = axial + lateral + yaw;
        double rightFrontPower = axial - lateral - yaw;
        double leftBackPower = axial - lateral + yaw;
        double rightBackPower = axial + lateral - yaw;
        // Normalize the values so no wheel power exceeds 100%
        max = Math.max(Math.abs(leftFrontPower), Math.abs(rightFrontPower));
        max = Math.max(max, Math.abs(leftBackPower));
        max = Math.max(max, Math.abs(rightBackPower));

        if (max > 1.0) {
            leftFrontPower /= max;
            rightFrontPower /= max;
            leftBackPower /= max;
            rightBackPower /= max;
        }

        double lfspeed = leftFrontPower * speed;
        double rfspeed = rightFrontPower * speed;
        double lbspeed = leftBackPower * speed;
        double rbspeed = rightBackPower * speed;

        leftFront.setPower(lfspeed);
        rightFront.setPower(rfspeed);
        leftRear.setPower(lbspeed);
        rightRear.setPower(rbspeed);

        if (gamepad2.b)
            intake.setSpinState(IntakeSubsystem.IntakeSpinState.IN, false);
        else if (gamepad2.dpad_down)
            intake.setSpinState(IntakeSubsystem.IntakeSpinState.OUT, false);
        else
            intake.setSpinState(IntakeSubsystem.IntakeSpinState.STOP, false);

        if(currentGamepad1.a && !previousGamepad1.a)
            intake.switchPivotState();

        if (gamepad2.right_bumper)
            extend.manual(1);
        else if (gamepad2.left_bumper)
            extend.manual(-1);
        else
            extend.manual(0);

        if (currentGamepad2.a && !previousGamepad2.a)
            claw.switchGrabState();

        if (currentGamepad2.y && !previousGamepad2.y)
            transferPos();

        if (currentGamepad2.x && !previousGamepad2.x)
            scoringPos();

        if (currentGamepad2.dpad_left && !previousGamepad2.dpad_left)
            specimenPos();

        if (currentGamepad2.dpad_right && !previousGamepad2.dpad_right)
            chamberPos();

        if (currentGamepad2.dpad_up && !previousGamepad2.dpad_up)
            claw.specimenScore();

        if (currentGamepad1.b && !previousGamepad1.b)
            intake.setPivotState(IntakeSubsystem.IntakePivotState.TRANSFER);

        //follower.setTeleOpMovementVectors(-gamepad1.left_stick_y * speed, -gamepad1.left_stick_x * speed, -gamepad1.right_stick_x * speed, !fieldCentric);
        //follower.update();

        //telemetry.addData("X", follower.getPose().getX());
        //telemetry.addData("Y", follower.getPose().getY());
        //telemetry.addData("Heading", Math.toDegrees(follower.getPose().getHeading()));

        telemetry.addData("Left Lift Pos", lift.leftLift.getCurrentPosition());
        telemetry.addData("Right Lift Pos", lift.rightLift.getCurrentPosition());
telemetry.addData(" Extend Pos", extend.leftExtend.getPosition());
        telemetry.addData("Claw Grab State", claw.grabState);
        telemetry.addData("Claw Pivot State", claw.pivotState);
     //   telemetry.addData("Intake Spin State", intakeSpinState);
     //   telemetry.addData("Intake Pivot State", intakePivotState);
        telemetry.addData("arm State", arm.state);
        telemetry.update();
    }

    public void start() {
        claw.start();
        //lift.start();
        extend.start();
        intake.start();
        arm.start();
        //follower.setPose(startPose);
        //follower.startTeleopDrive();
    }

    private void startDrive() {
        //follower.startTeleopDrive();
    }

    private void stopDrive(){
        //follower.breakFollowing();
    }

    /*public Action transfer() {
        return new SequentialAction(
                stopDrive,
                //intake.spinStop,
                new ParallelAction(
                        //intake.pivotTransfer,
                        extend.toZero,
                        claw.open,
                        //lift.toZero,
                        arm.toTransfer),
                //intake.spinOut,
                new SleepAction(1),
               // intake.spinIn,
                //intake.spinStop,
                startDrive
        );
    }*/

    private void scoringPos() {
        claw.score();
        claw.close();
        arm.score();
    }

    private void transferPos() {
        claw.transfer();
        claw.open();
        arm.transfer();
    }

    private void specimenPos() {
        claw.specimen();
        claw.open();
        arm.specimen();
    }

    private void chamberPos() {
        claw.specimen();
        claw.close();
        arm.specimen();
    }

    private void initPos() {
        transferPos();
    }

}