import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BookingForm } from './BookingForm';

const rooms = [
  { id: 1, capacity: 2, pricePerPersonPerHour: 8 },
  { id: 2, capacity: 6, pricePerPersonPerHour: 12 },
];
const total = () => screen.getByRole('status', { name: 'Estimated total' });
const equipment = [{ id: 7, name: 'PROJECTOR abc', price: 20, typeOfEquipment: 'PROJECTOR' as const }];

function setup(onSubmit = vi.fn()) {
  render(<BookingForm rooms={rooms} equipment={equipment} pending={false} onSubmit={onSubmit} />);
  return { onSubmit, user: userEvent.setup() };
}

describe('BookingForm', () => {
  it('shows a live estimate that follows group size and equipment', async () => {
    const { user } = setup();
    // 1 person x 2 hours in the 8 TND room
    expect(total()).toHaveTextContent('16.00 TND');

    const people = screen.getByLabelText('People');
    await user.clear(people);
    await user.type(people, '4');
    // the 2-seat room no longer fits: 12 x 4 x 2
    expect(total()).toHaveTextContent('96.00 TND');

    await user.click(screen.getByRole('checkbox', { name: /Projector/ }));
    // + 20 per hour x 2
    expect(total()).toHaveTextContent('136.00 TND');
  });

  it('explains when no room is big enough and disables booking', async () => {
    const { user } = setup();
    const people = screen.getByLabelText('People');
    await user.clear(people);
    await user.type(people, '9');
    expect(screen.getByText(/No room here seats 9 people \(largest: 6\)/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Request booking' })).toBeDisabled();
  });

  it('requires a date and focuses it', async () => {
    const { user, onSubmit } = setup();
    await user.click(screen.getByRole('button', { name: 'Request booking' }));
    expect(screen.getByText('Choose a date.')).toBeInTheDocument();
    expect(screen.getByLabelText('Date')).toHaveFocus();
    expect(screen.getByLabelText('Date')).toHaveAttribute('aria-invalid', 'true');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('refuses bookings that run past midnight', async () => {
    const { user, onSubmit } = setup();
    await user.type(screen.getByLabelText('Date'), '2099-01-15');
    const time = screen.getByLabelText('Start time');
    await user.clear(time);
    await user.type(time, '23:00');
    await user.click(screen.getByRole('button', { name: 'Request booking' }));
    expect(screen.getByText(/Must end by midnight: start by 22:00/)).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits the chosen values', async () => {
    const { user, onSubmit } = setup();
    await user.type(screen.getByLabelText('Date'), '2099-01-15');
    await user.selectOptions(screen.getByLabelText('Duration'), '3');
    await user.click(screen.getByRole('checkbox', { name: /Projector/ }));
    await user.click(screen.getByRole('button', { name: 'Request booking' }));
    expect(onSubmit).toHaveBeenCalledWith({ date: '2099-01-15', time: '09:00', hours: 3, numberOfPeople: 1, equipmentIds: [7] });
  });
});
