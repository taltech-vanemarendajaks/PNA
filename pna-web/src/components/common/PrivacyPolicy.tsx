export function PrivacyPolicy() {
  return (
    <section className="mx-auto w-full rounded-4xl border border-base-300 bg-base-100 p-8 shadow-xl shadow-primary/5 sm:p-10">
      <span className="badge badge-primary mb-4">Privacy Policy</span>
      <p className="mb-4">
        This privacy policy explains how pna.ee processes and protects user data.
      </p>

      <p className="text-xl text-primary font-semibold my-4">1. Nature of the Service</p>
      <p>pna.ee lets users quickly search information about phone numbers.</p>
      <br />
      <p>
        We collect and store identifying data only when needed to provide the service, ensure
        security, or meet legal obligations.
      </p>

      <p className="text-xl text-primary font-semibold my-4">2. Data Processed</p>
      <p>When using the service, we may process:</p>
      <ul className="list-disc">
        <li className="ml-8">
          technical data, such as IP address, browser, device info, and server logs;
        </li>
        <li className="ml-8">user-provided data, such as name and email address;</li>
        <li className="ml-8">activity data, such as search history and account usage.</li>
      </ul>
      <p>
        Technical data is collected automatically to keep the service secure and prevent misuse.
      </p>

      <p className="text-xl text-primary font-semibold my-4">3. Purpose of Data Processing</p>
      <p>We use data only to provide and manage the service, including to:</p>
      <ul className="list-disc">
        <li className="ml-8">provide access to pna.ee;</li>
        <li className="ml-8">show previous searches;</li>
        <li className="ml-8">manage user accounts;</li>
        <li className="ml-8">improve reliability and security;</li>
        <li className="ml-8">prevent misuse or technical issues;</li>
        <li className="ml-8">meet legal obligations.</li>
      </ul>
      <p>We do not use personal data for marketing or advertising without prior consent.</p>

      <p className="text-xl text-primary font-semibold my-4">4. Content from Public Sources</p>
      <p>Information shown on pna.ee come from public internet sources.</p>
      <br />
      <p>
        pna.ee does not guarantee that such information is complete, current, accurate, or lawful.
        Users are responsible for how they use it.
      </p>

      <p className="text-xl text-primary font-semibold my-4">5. Data Retention</p>
      <p>
        We keep data only as long as needed to provide the service, ensure security, or meet legal
        obligations.
      </p>
      <br />
      <p>Users may request account and data deletion unless the law requires further retention.</p>

      <p className="text-xl text-primary font-semibold my-4">6. Sharing Data with Third Parties</p>
      <p>We do not sell, rent, or share personal data with third parties for marketing purposes.</p>
      <br />
      <p>Data may be shared only:</p>
      <ul className="list-disc">
        <li className="ml-8">
          when needed for technical operation, such as hosting or server services;
        </li>
        <li className="ml-8">when the user has given consent;</li>
        <li className="ml-8">when required by law, such as by law enforcement.</li>
      </ul>

      <p className="text-xl text-primary font-semibold my-4">7. Security</p>
      <p>
        We use reasonable technical and organizational measures to protect user data. However, data
        transmission over the internet is never fully risk-free, and absolute security cannot be
        guaranteed.
      </p>

      <p className="text-xl text-primary font-semibold my-4">8. User Rights</p>
      <p>The user has the right to:</p>
      <ul className="list-disc">
        <li className="ml-8">access their processed data;</li>
        <li className="ml-8">request correction of inaccurate data;</li>
        <li className="ml-8">request deletion of their data;</li>
        <li className="ml-8">withdraw consent where processing is based on consent;</li>
        <li className="ml-8">object to processing where allowed by law;</li>
        <li className="ml-8">
          complain to the Data Protection Inspectorate about unlawful processing.
        </li>
      </ul>

      <p className="text-xl text-primary font-semibold my-4">9. Changes to the Privacy Terms</p>
      <p>We may update this policy to reflect service changes or legal requirements.</p>
      <p>
        Changes apply once published on the website. Continued use of the service means the user has
        reviewed the updated terms.
      </p>

      <div className="flex justify-center mt-4">Last updated: 26.04.2026</div>
    </section>
  );
}
