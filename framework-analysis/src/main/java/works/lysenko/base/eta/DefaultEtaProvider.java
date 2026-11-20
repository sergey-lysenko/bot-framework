package works.lysenko.base.eta;

import works.lysenko.base.eta.model.EtaSnapshot;
import works.lysenko.util.apis.core._EtaProvider;
import works.lysenko.util.apis.scenario._Scenario;

/**
 * Default {@link _EtaProvider} implementation wiring ETA calculation, calibration, and sidecar recording.
 */
public final class DefaultEtaProvider implements _EtaProvider {

    @Override
    public void onReset() {
        AllLeafCompletions.reset();
        EtaDebug.reset();
        EtaCorrection.reset();
    }

    @Override
    public void onCycleStart(final String shortName) {
        EtaDebug.startCycle(shortName);
    }

    @Override
    public void onCycleEnd(final String shortName, final long elapsedMs, final _Scenario completedLeaf) {
        EtaDebug.endCycle(shortName);
        AllLeafCompletions.checkAndRecord(elapsedMs, completedLeaf);
    }

    @Override
    public void onLimboCycle(final Integer testNumber, final long elapsedMs) {
        AllLeafCompletions.recordCycleIfMissing(testNumber, elapsedMs);
    }

    @Override
    public void onRunCompletion(final long totalRuntimeMs) {
        EtaCorrection.recordRunCompletion(totalRuntimeMs);
    }

    @Override
    public String captureEtaJson(final boolean markDone, final long nowMs, final long elapsedMs, final String title) {
        EtaSnapshot snapshot = EtaCalculator.capture(true);
        if (!markDone && snapshot.remainingMs() > 0L) {
            AllLeafCompletions.recordEtaSample(snapshot.remainingMs());
        }
        if (markDone) {
            snapshot = snapshot.asDone(nowMs, Math.max(0L, elapsedMs));
        }
        return snapshot.toJson(title);
    }
}
