package frc.robot.subsystems.shooter.flywheels;

import static edu.wpi.first.units.Units.*;

// import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import frc.robot.Constants;
import frc.robot.util.GeneralUtil;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;

public class Flywheels extends SubsystemBase {
  private final FlywheelsIO io;
  private final FlywheelsIOInputsAutoLogged inputs = new FlywheelsIOInputsAutoLogged();
  // private final SimpleMotorFeedforward ffModelTop;
  // private final SimpleMotorFeedforward ffModelBottom;
  private final SysIdRoutine sysIdTop;
  private final SysIdRoutine sysIdBottom;

  private final double kFlywheelIntakeSpeedVoltage = -2;
  private final double kAmpShootingSpeedBottomVoltage = 3.5;
  private final double kAmpShootingSpeedTopVoltage = 0.5;
  private final double kShooterDefaultSpeedVoltage = 10;
  private final double kShooterFeedingSpeedVoltage = 5;

  private static enum LastGoal {
    NONE,
    AMP,
    SPEAKER,
    FEEDING
  }
  private LastGoal lastGoal = LastGoal.NONE;

  /** Creates a new Flywheel. */
  public Flywheels(FlywheelsIO io) {
    this.io = io;

    // Switch constants based on mode (the physics simulator is treated as a
    // separate robot with different tuning)
    switch (Constants.currentMode) {
      case REAL:
      case REPLAY:
        // ffModelTop = new SimpleMotorFeedforward(0.1, 0.05);
        // ffModelBottom = new SimpleMotorFeedforward(0.1, 0.05);
        io.configurePID(1.0, 0.0, 0.0);
        break;
      case SIM:
        // ffModelTop = new SimpleMotorFeedforward(0.0, 0.03);
        // ffModelBottom = new SimpleMotorFeedforward(0.0, 0.03);
        io.configurePID(0.5, 0.0, 0.0);
        break;
      default:
        // ffModelTop = new SimpleMotorFeedforward(0.0, 0.0);
        // ffModelBottom = new SimpleMotorFeedforward(0.0, 0.0);
        break;
    }

    // Configure SysId
    sysIdTop =
        new SysIdRoutine(
            new SysIdRoutine.Config(
                null,
                null,
                null,
                (state) -> Logger.recordOutput("Flywheels/Top/SysIdState", state.toString())),
            new SysIdRoutine.Mechanism((voltage) -> runVoltsTop(voltage.in(Volts)), null, this));

    sysIdBottom =
        new SysIdRoutine(
            new SysIdRoutine.Config(
                null,
                null,
                null,
                (state) -> Logger.recordOutput("Flywheels/Bottom/SysIdState", state.toString())),
            new SysIdRoutine.Mechanism((voltage) -> runVoltsBottom(voltage.in(Volts)), null, this));
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Shooter/Flywheels", inputs);

    // Logs
    GeneralUtil.logSubsystem(this, "Shooter/Flywheels");
  }

  /** Run open loop at the specified voltage. */
  private void runVoltsTop(double volts) {
    io.setVoltageTop(volts);
  }

  /** Run open loop at the specified voltage. */
  private void runVoltsBottom(double volts) {
    io.setVoltageBottom(volts);
  }

  private void runVoltsBoth(double volts) {
    runVoltsTop(volts);
    runVoltsBottom(volts);
  }

  // /** Run closed loop at the specified velocity. */
  // private void runVelocityTop(double velocityRPM) {
  //   var velocityRadPerSec = Units.rotationsPerMinuteToRadiansPerSecond(velocityRPM);
  //   io.setVelocityTop(velocityRadPerSec, ffModelTop.calculate(velocityRadPerSec));

  //   // Log flywheels setpoint
  //   Logger.recordOutput("Flywheel/Top/SetpointRPM", velocityRPM);
  // }

  // /** Run closed loop at the specified velocity. */
  // private void runVelocityBottom(double velocityRPM) {
  //   var velocityRadPerSec = Units.rotationsPerMinuteToRadiansPerSecond(velocityRPM);
  //   io.setVelocityBottom(velocityRadPerSec, ffModelBottom.calculate(velocityRadPerSec));

  //   // Log flywheels setpoint
  //   Logger.recordOutput("Flywheel/Bottom/SetpointRPM", velocityRPM);
  // }

  private void stopBoth() {
    io.stopBoth();
  }

  public Command intake(boolean intakeWorking) {
    return Commands.startEnd(() -> {
      if (intakeWorking) {
        runVoltsBoth(kShooterDefaultSpeedVoltage / 2);
      } else {
        runVoltsBoth(kFlywheelIntakeSpeedVoltage);
      }
    }, () -> {
      switch (lastGoal) {
        case AMP:
          shootAmp();
          break;
        case SPEAKER:
          shootSpeaker();
          break;
        case FEEDING:
          feed();
          break;
        default:
          stop();
          break;
      }
    }).withName("Flywheels Intake");
  }

  public Command shootAmp() {
    return runOnce(() -> {
      runVoltsTop(kAmpShootingSpeedTopVoltage);
      runVoltsBottom(kAmpShootingSpeedBottomVoltage);
      lastGoal = LastGoal.AMP;
    }).withName("Flywheels Shoot Amp");
  }

  public Command shootSpeaker() {
    return runOnce(() -> {
      runVoltsBoth(kShooterDefaultSpeedVoltage);
      lastGoal = LastGoal.SPEAKER;
    }).withName("Flywheels Shoot Speaker");
  }

  public Command feed() {
    return runOnce(() -> {
      runVoltsBoth(kShooterFeedingSpeedVoltage);
      lastGoal = LastGoal.FEEDING;
    }).withName("Flywheels Feed");
  }

  public Command stop() {
    return runOnce(() -> {
      stopBoth();
      lastGoal = LastGoal.NONE;
    }).withName("Flywheels Stop");
  }
  
  // TODO make a default cmd that stops if on opponents side of field when this merges with the advanced branch

  /** Returns a command to run a quasistatic test in the specified direction. */
  public Command sysIdQuasistaticTop(SysIdRoutine.Direction direction) {
    return sysIdTop.quasistatic(direction);
  }

  /** Returns a command to run a dynamic test in the specified direction. */
  public Command sysIdDynamicTop(SysIdRoutine.Direction direction) {
    return sysIdTop.dynamic(direction);
  }

  /** Returns a command to run a quasistatic test in the specified direction. */
  public Command sysIdQuasistaticBottom(SysIdRoutine.Direction direction) {
    return sysIdBottom.quasistatic(direction);
  }

  /** Returns a command to run a dynamic test in the specified direction. */
  public Command sysIdDynamicBottom(SysIdRoutine.Direction direction) {
    return sysIdBottom.dynamic(direction);
  }

  /** Returns the current velocity in RPM. */
  @AutoLogOutput
  public double getVelocityRPMTop() {
    return Units.radiansPerSecondToRotationsPerMinute(inputs.velocityRadPerSecTop);
  }

  /** Returns the current velocity in RPM. */
  @AutoLogOutput
  public double getVelocityRPMBottom() {
    return Units.radiansPerSecondToRotationsPerMinute(inputs.velocityRadPerSecBottom);
  }
}
