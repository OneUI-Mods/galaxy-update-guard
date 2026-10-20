package dev.ryuya.galaxyupdateguard;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import org.junit.Test;

public class UpdateBlockerTest {
  private static final String FIRST = UpdatePackages.ALL.get(0), SECOND = UpdatePackages.ALL.get(1);

  static class Fake implements UpdateBlocker.Backend, UpdateBlocker.Store {
    Map<String, Integer> values = new LinkedHashMap<>(Map.of(FIRST, 0, SECOND, 1));
    UpdateBlocker.Journal journal;
    String fail;
    boolean persistFails, refuseChange;
    int writes;

    public Map<String, Integer> states() {
      return Map.copyOf(values);
    }

    public void set(String name, int state) throws Exception {
      writes++;
      if (Objects.equals(fail, name)) {
        fail = null;
        throw new IOException("One write failed");
      }
      if (!refuseChange) values.put(name, state);
    }

    public UpdateBlocker.Journal load() {
      return journal;
    }

    public void save(UpdateBlocker.Journal next) throws Exception {
      if (persistFails) throw new IOException("Disk full");
      journal = next;
    }

    public void clear() {
      journal = null;
    }

    UpdateBlocker controller() {
      return new UpdateBlocker(this, this);
    }
  }

  @Test
  public void roundTripRestoresDefaultAndExplicitEnabled() throws Exception {
    Fake fake = new Fake();
    var original = Map.copyOf(fake.values);
    fake.controller().block();
    assertEquals(Map.of(FIRST, 3, SECOND, 3), fake.values);
    assertTrue(fake.journal.requested());
    fake.controller().restore();
    assertEquals(original, fake.values);
    assertNull(fake.journal);
  }

  @Test
  public void initiallyDisabledServicesStayDisabledAfterRestore() throws Exception {
    Fake fake = new Fake();
    fake.values.put(SECOND, 2);
    fake.controller().block();
    fake.controller().restore();
    assertEquals(Integer.valueOf(2), fake.values.get(SECOND));
  }

  @Test
  public void repeatedBlockNeverOverwritesOriginalState() throws Exception {
    Fake fake = new Fake();
    fake.controller().block();
    fake.controller().block();
    fake.controller().restore();
    assertEquals(Integer.valueOf(0), fake.values.get(FIRST));
  }

  @Test
  public void failedBlockRollsBackEveryTarget() throws Exception {
    Fake fake = new Fake();
    var original = Map.copyOf(fake.values);
    fake.fail = SECOND;
    assertThrows(IOException.class, () -> fake.controller().block());
    assertEquals(original, fake.values);
    assertNull(fake.journal);
  }

  @Test
  public void failedSaveCannotChangePackages() {
    Fake fake = new Fake();
    fake.persistFails = true;
    assertThrows(IOException.class, () -> fake.controller().block());
    assertEquals(0, fake.writes);
  }

  @Test
  public void restoreFailureRetainsOriginalStateForRetry() throws Exception {
    Fake fake = new Fake();
    fake.controller().block();
    fake.fail = FIRST;
    assertThrows(IOException.class, () -> fake.controller().restore());
    assertNotNull(fake.journal);
    assertFalse(fake.journal.requested());
    fake.controller().restore();
    assertNull(fake.journal);
    assertEquals(Integer.valueOf(0), fake.values.get(FIRST));
  }

  @Test
  public void processDeathDuringBlockCanBeRecovered() throws Exception {
    Fake fake = new Fake();
    var original = Map.copyOf(fake.values);
    fake.save(new UpdateBlocker.Journal(original, true));
    fake.values.put(FIRST, 3);
    fake.controller().restore();
    assertEquals(original, fake.values);
    assertNull(fake.journal);
  }

  @Test
  public void unownedDisableCannotBeUndone() throws Exception {
    Fake fake = new Fake();
    fake.values.put(FIRST, 3);
    fake.controller().restore();
    assertEquals(0, fake.writes);
    assertEquals(Integer.valueOf(3), fake.values.get(FIRST));
  }

  @Test
  public void verificationFailureIsNotSuccess() {
    Fake fake = new Fake();
    fake.refuseChange = true;
    assertThrows(IllegalStateException.class, () -> fake.controller().block());
    assertNull(fake.journal);
  }

  @Test
  public void commandScopeRejectsUnrelatedPackagesAndInvalidStates() {
    assertThrows(
        IllegalArgumentException.class,
        () -> UpdatePackages.command("com.google.android.modulemetadata", 3));
    assertThrows(
        IllegalArgumentException.class, () -> UpdatePackages.command(FIRST + "; reboot", 3));
    assertThrows(IllegalArgumentException.class, () -> UpdatePackages.command(FIRST, 5));
    assertTrue(UpdatePackages.command(FIRST, 0).contains("default-state --user 0"));
  }

  @Test
  public void allFiveOriginalStatesRoundTrip() throws Exception {
    for (int state = 0; state <= 4; state++) {
      Fake fake = new Fake();
      fake.values.put(FIRST, state);
      fake.controller().block();
      fake.controller().restore();
      assertEquals(Integer.valueOf(state), fake.values.get(FIRST));
    }
  }

  @Test
  public void missingUpdaterDoesNotEraseRestoreJournal() throws Exception {
    Fake fake = new Fake();
    fake.controller().block();
    fake.values.remove(FIRST);
    fake.refuseChange = true;
    assertThrows(IllegalStateException.class, () -> fake.controller().restore());
    assertNotNull(fake.journal);
  }
}
