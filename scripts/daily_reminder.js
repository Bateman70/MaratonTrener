const https = require('https');

// Helper to perform HTTPS GET
function getJson(url) {
    return new Promise((resolve, reject) => {
        https.get(url, (res) => {
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch (e) {
                    reject(e);
                }
            });
        }).on('error', reject);
    });
}

// Helper to perform HTTPS POST (sending ntfy alert)
function sendNtfy(topic, title, message) {
    return new Promise((resolve, reject) => {
        const url = `https://ntfy.sh/${topic}`;
        const req = https.request(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain; charset=utf-8',
                'Title': `=?UTF-8?B?${Buffer.from(title).toString('base64')}?=`,
                'Tags': 'runner,bell'
            }
        }, (res) => {
            resolve(res.statusCode);
        });
        req.on('error', reject);
        req.write(message);
        req.end();
    });
}

async function run() {
    try {
        console.log("Fetching workouts from Firebase...");
        const workoutsData = await getJson('https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/workouts.json');
        if (!workoutsData) {
            console.log("No workouts found in database.");
            return;
        }

        // Get current date in Europe/Oslo timezone
        const now = new Date();
        const osloTimeStr = now.toLocaleString("en-US", { timeZone: "Europe/Oslo" });
        const osloDate = new Date(osloTimeStr);
        const year = osloDate.getFullYear();
        const month = String(osloDate.getMonth() + 1).padStart(2, '0');
        const day = String(osloDate.getDate()).padStart(2, '0');
        const todayStr = `${year}-${month}-${day}`;
        console.log(`Checking workouts for date: ${todayStr} (Norway Time)`);

        const userIds = Object.keys(workoutsData);
        for (const userId of userIds) {
            const userWorkouts = workoutsData[userId];
            if (!userWorkouts) continue;

            // Find an incomplete workout scheduled for today
            let todaysWorkout = null;
            const workoutKeys = Object.keys(userWorkouts);
            for (const key of workoutKeys) {
                const w = userWorkouts[key];
                if (w && w.scheduledDate === todayStr && !w.isCompleted) {
                    todaysWorkout = w;
                    break;
                }
            }

            if (todaysWorkout) {
                const topic = `maratontrener-alerts-${userId.toLowerCase()}`;
                const title = `Dagens treningsøkt! 🏃‍♂️`;
                const workoutType = todaysWorkout.workoutType || 'Løpetur';
                const distance = parseFloat(todaysWorkout.distance || 0);
                const message = `Du har planlagt en økt i dag: ${workoutType}${distance > 0 ? ` på ${distance} km` : ''}. God tur!`;

                console.log(`Sending alert for user ${userId} on topic ${topic}...`);
                const status = await sendNtfy(topic, title, message);
                console.log(`Sent alert. ntfy response status: ${status}`);
            } else {
                console.log(`No scheduled workout today for user ${userId}.`);
            }
        }
    } catch (err) {
        console.error("Error in daily reminder execution:", err);
        process.exit(1);
    }
}

run();
