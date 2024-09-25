This directory contains routes and scripts that enable communication between PingAM and PingGateway, specifically for the Google Chrome Device Trust node. 

Follow this quick setup guide to test the functionality.

---

PingGateway Redirect Routes Setup:
1. Install PingGateway
    - [Quick Install Guide](https://backstage.forgerock.com/docs/ig/2024.6/getting-started/preface.html)
2. Configure the `admin.json` file to run PingGateway on ports 9090 / 9443
    - [Startup PingGateway with custom settings](https://backstage.forgerock.com/docs/ig/2024.6/installation-guide/start-stop.html#starting-options)
3. Store the JSON route files inside the routes directory
    - E.g. `/Users/your-username/.openig/config/routes`
4. Store the groovy script inside the groovy directory
    - E.g. `/Users/your-username/.openig/scripts/groovy`
        - If you do not have a `groovy` directory create one

---

Simplified Explanation:
1. The 1st route (`pingam.json`) makes an API call to Google to generate the challenge
   - This route makes the API call by running the `getChallenge.groovy` script 
2. The 1st route then redirects to the 2nd route (`challengeresponse.json`) and performs the following:
    - Retrieves the challenge response value
    - Stores the challenge inside the redirect URL as a query parameter
    - Redirects back to PingAM

---

Tips:
1. Open the `pingam.json` route and check the `condition` field. Make sure this matches your current PingAM version. 
2. Open the `challengeresponse.json` route and swap out the `location` redirect URL to match your desired location.
   - Do not remove the `challengeresponse` query parameter at the end of the redirect.

