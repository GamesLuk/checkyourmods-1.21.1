package checkyourmods.main;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JoinFailureInfo {
    LocalDateTime timestamp;
    List<String> missingMods = new ArrayList<>();
    List<String> unallowedMods = new ArrayList<>();
}
