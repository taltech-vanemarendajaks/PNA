import { ThemeController } from "../common/ThemeController";

export function SettingsComponent() {
  return (
    <section className="mx-auto w-full rounded-4xl border border-base-300 bg-base-100 p-8 shadow-xl shadow-primary/5 sm:p-10">
      <div className="max-w-3xl">
        <span className="badge badge-primary mb-4">Settings</span>
        <div className="flex flex-col gap-6">
          <div className="flex flex-col gap-4">
            <p className="text-xl text-primary font-semibold">Appearence</p>
            <div className="flex justify-between">
              <p className="text-md font-semibold">Theme</p>
              <ThemeController />
            </div>
          </div>
          <hr />
          <div className="flex flex-col gap-4">
            <p className="text-xl text-error font-semibold">Danger Zone</p>
            <div className="flex justify-between items-center">
              <div>
                <p className="text-md font-semibold">Remove Data</p>
                <span>Clear all number search history</span>
              </div>
              <button type="button" className="btn btn-error">
                Remove
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
