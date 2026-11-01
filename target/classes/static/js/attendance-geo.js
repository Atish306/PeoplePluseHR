/**
 * Handles browser geolocation capture for employee attendance punch-in
 */
document.addEventListener("DOMContentLoaded", () => {
    const punchInForm = document.getElementById("punchInForm");
    const punchInBtn = document.getElementById("punchInBtn");
    const geoStatus = document.getElementById("geoStatus");
    const latInput = document.getElementById("latitudeInput");
    const lonInput = document.getElementById("longitudeInput");

    if (punchInForm && punchInBtn) {
        punchInBtn.addEventListener("click", (e) => {
            e.preventDefault();

            punchInBtn.disabled = true;
            punchInBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span> Getting Location...';
            if (geoStatus) geoStatus.innerText = "Requesting GPS coordinates...";

            if ("geolocation" in navigator) {
                navigator.geolocation.getCurrentPosition(
                    (position) => {
                        if (latInput) latInput.value = position.coords.latitude;
                        if (lonInput) lonInput.value = position.coords.longitude;
                        if (geoStatus) {
                            geoStatus.className = "small text-success mt-2";
                            geoStatus.innerText = "Location verified. Submitting check-in...";
                        }
                        punchInForm.submit();
                    },
                    (error) => {
                        console.warn("Geolocation prompt was dismissed or failed:", error.message);
                        if (geoStatus) {
                            geoStatus.className = "small text-muted mt-2";
                            geoStatus.innerText = "Proceeding without GPS coordinates...";
                        }
                        // Proceed with punch-in even if GPS is denied
                        punchInForm.submit();
                    },
                    {
                        timeout: 5000,
                        maximumAge: 60000,
                        enableHighAccuracy: true
                    }
                );
            } else {
                // Browser does not support geolocation
                punchInForm.submit();
            }
        });
    }
});
