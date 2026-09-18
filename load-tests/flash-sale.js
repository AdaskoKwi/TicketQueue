import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        flash_sale: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '5s', target: 500},
                { duration: '10s', target: 500},
                { duration: '5s', target: 0}
            ],
        },
    },
};

export default function () {
    const userId = `user-${__VU}-${__ITER}`;
    const res = http.post(
        'http://localhost:8080/events/concert-2026/queue/join',
        JSON.stringify({ userId }),
        { headers: { 'Content-Type': 'application/json' }}
    );

    check(res, {
        'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    });
}