import http from 'k6/http';
import { check } from 'k6';

export const options = {
    // The duration of the test in seconds
    duration: '30s',
    // The number of virtual users to simulate during the test
    vus: 10,
};

export default function () {
    // The URL for the POST request
    const url = 'http://localhost:8081/request-registration';
    // The request payload (email in this case)
    const payload = JSON.stringify({ email: 'test@example.com' });
    const params = {
        headers: {
            // The request header specifying the content type as JSON
            'Content-Type': 'application/json',
        },
    };

    // Send a POST request to the specified URL with payload and headers
    const res = http.post(url, payload, params);

    check(res, {
        // Assert that the response status is 204 (No Content)
        'Status is 204': (r) => r.status === 204,
    });
}
