package frc.robot.subsystems.shooter.pivot;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import frc.robot.Constants;

public class PivotConstants {

  public static final int angleMotorId = 0;
  public static final double kSpeakerManualAngleRevRotations = -9.5;
  public static final double kDesiredAmpAngleRevRotations = -8;
  public static final double kSourceAngleRevRotations = -1;
  public static final double kFeedingAngleRevRotations = -5;
  public static final double kIntakeAngleRevRotations = -63;
  public static final double kSpeakerAngleOffsetRevRotations = -74;
  public static final double kDesiredIntakeAngleRevRotations = -63;
  public static final double kDesiredEjectAngleRevRotations = 0;
  // TODO: find this value
  public static final double kDesiredSourceIntakeRevRotations = 0;

  public static final double pivotLength = Units.inchesToMeters(13.835);
  public static final Translation2d pivotOrigin = new Translation2d(.263, .28);

  public static final Gains gains =
      switch (Constants.currentMode) {
        default -> new Gains(0.15);
        case SIM -> new Gains(20);
      };

  public record Gains(double kP) {}
}
